package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.PaymentTransaction;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.PaymentTransactionRepository;
import com.trainning.movie_booking_system.service.PaymentService;
import com.trainning.movie_booking_system.service.VnPayService;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentServiceImpl - Implement PaymentService, xử lý logic thanh toán
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final SeatDomainService seatDomainService;
    private final VnPayService vnPayService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    /**
     * Tạo payment URL và redirect user sang gateway
     */
    @Override
    @Transactional
    public String createPaymentUrl(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Booking is not in PENDING_PAYMENT status. Current status: " + booking.getStatus()
            );
        }

        String txnRef = "TXN_" + System.currentTimeMillis() + "_" + bookingId;

        PaymentTransaction transaction = PaymentTransaction.builder()
                .booking(booking)
                .gatewayType(com.trainning.movie_booking_system.utils.enums.PaymentGatewayType.VNPAY)
                .transactionId(txnRef)
                .amount(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .ipAddress("127.0.0.1")
                .initiatedAt(LocalDateTime.now())
                .build();

        paymentTransactionRepository.save(transaction);

        long amountVnd = booking.getFinalAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        String orderInfo = "Thanh toan ve xem phim - Booking #" + bookingId;
        String clientIp = "127.0.0.1";

        return vnPayService.createPaymentUrl(txnRef, amountVnd, orderInfo, clientIp);
    }

    /**
     * Xử lý callback từ VNPay (Return URL hoặc IPN)
     * Internal method, KHÔNG override interface
     */
    @Transactional
    public PaymentResponse processPaymentCallback(PaymentRequest request) {
        // Verify signature VNPay trước khi update
        if (!vnPayService.verifySignature(request)) {
            throw new BadRequestException("Invalid payment signature");
        }

        PaymentTransaction transaction = paymentTransactionRepository
                .findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + request.getTransactionId()));

        // Idempotency: nếu transaction đã SUCCESS/FAILED thì không xử lý lại
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            return buildResponse(transaction.getBooking(), "SUCCESS", "Payment already completed");
        } else if (transaction.getStatus() == PaymentStatus.FAILED) {
            return buildResponse(transaction.getBooking(), "FAILED", "Payment already failed");
        }

        Booking booking = transaction.getBooking();

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setGatewayOrderId(request.getGatewayOrderId());
            transaction.setPaymentMethod(request.getPaymentMethod());
            transaction.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.consumeHoldToBooked(booking.getShowtime().getId(), seatIds);

            log.info("[PAYMENT] Payment SUCCESS for booking {}", booking.getId());
            return buildResponse(booking, "SUCCESS", "Payment completed successfully");

        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

            log.warn("[PAYMENT] Payment FAILED for booking {}", booking.getId());
            return buildResponse(booking, "FAILED", "Payment failed or cancelled");
        }
    }

    /**
     * Xử lý callback VNPay Return URL
     */
    @Override
    public PaymentResponse handleVNPayReturn(jakarta.servlet.http.HttpServletRequest request) {
        PaymentRequest paymentRequest = vnPayService.parseRequest(request);
        return processPaymentCallback(paymentRequest);
    }

    /**
     * Xử lý callback cũ / DEPRECATED
     */
    @Override
    public PaymentResponse handlePaymentCallback(PaymentRequest request) {
        return processPaymentCallback(request);
    }

    /**
     * Verify payment status
     */
    @Override
    public String verifyPaymentStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        return booking.getStatus().name();
    }

    /**
     * Cancel payment
     */
    @Override
    @Transactional
    public void cancelPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel confirmed booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
        seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);
    }

    /**
     * Cron job tự động release booking PENDING_PAYMENT quá 15 phút
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void releaseExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.findAllByStatus(BookingStatus.PENDING_PAYMENT).forEach(booking -> {
            PaymentTransaction txn = paymentTransactionRepository
                    .findTopByBookingOrderByInitiatedAtDesc(booking)
                    .orElse(null);

            if (txn != null && Duration.between(txn.getInitiatedAt(), now).toMinutes() > 15) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                List<Long> seatIds = booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getId())
                        .toList();
                seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

                log.info("[PAYMENT] Booking {} expired due to timeout. Seats released.", booking.getId());
            }
        });
    }

    /** Helper build PaymentResponse */
    private PaymentResponse buildResponse(Booking booking, String status, String message) {
        return PaymentResponse.builder()
                .bookingId(booking.getId())
                .status(status)
                .message(message)
                .build();
    }
}
