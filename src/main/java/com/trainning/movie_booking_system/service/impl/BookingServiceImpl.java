package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ConflictException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.RedisLockService;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.mapper.BookingMapper;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.SecurityUtils;
import com.trainning.movie_booking_system.service.BookingService;
import com.trainning.movie_booking_system.service.IVoucherService;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import com.trainning.movie_booking_system.utils.enums.SeatType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final VoucherRepository voucherRepository;
    private final IVoucherService voucherService;
    private final BookingMapper bookingMapper;
    private final RedisLockService redisLockService;
    private final SeatDomainService seatClient;

    @Override
    public BookingResponse create(BookingRequest request) {
        log.info("[BOOKING] Create booking request: {}", request);

        var currentUser = SecurityUtils.getCurrentUserDetails();
        Long userId = currentUser.getAccount().getId();

        if (CollectionUtils.isEmpty(request.getSeatIds())) {
            throw new BadRequestException("Seat list must not be empty");
        }

        // 1. Validate showtime exists
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found: " + request.getShowtimeId()));
        validateShowTime(showtime);

        // 2. Check seats held by user
        seatClient.assertHeldByUser(request.getShowtimeId(), request.getSeatIds(), userId);

        // 3. Get seat info for price calculation
        List<SeatInfo> seatInfos = seatClient.getSeatInfos(request.getSeatIds());

        // 4. Acquire distributed locks
        List<Long> sortedSeatIds = request.getSeatIds().stream().sorted().toList();
        List<Long> lockedSeats = new ArrayList<>();
        try {
            for (Long seatId : sortedSeatIds) {
                if (!redisLockService.tryLockSeat(request.getShowtimeId(), seatId, 30, TimeUnit.SECONDS)) {
                    throw new ConflictException("Cannot lock seat %d. Please try again.".formatted(seatId));
                }
                lockedSeats.add(seatId);
            }

            // 5. Re-verify holds under lock
            seatClient.assertHeldByUser(request.getShowtimeId(), request.getSeatIds(), userId);

            // 6. Tính tổng tiền trước
            BigDecimal totalPrice = calculateTotalPrice(seatInfos, showtime);

            // 7. Tạo booking pending trước khi validate voucher
            Booking booking = Booking.builder()
                    .account(currentUser.getAccount())
                    .showtime(showtime)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .totalPrice(totalPrice)
                    .build();
            bookingRepository.save(booking);

            // 8. Xử lý voucher nếu có
            Voucher voucher = null;
            BigDecimal discount = BigDecimal.ZERO;

            // Chỉ xử lý nếu voucherId được cung cấp và không rỗng
            String voucherIdStr = request.getVoucherId();
            if(voucherIdStr != null ){
                voucherIdStr = voucherIdStr.trim();
            }
            if (request.getVoucherId() != null && !request.getVoucherId().isEmpty()) {
                Long voucherId;
                try {
                    voucherId = Long.parseLong(request.getVoucherId());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Voucher ID must be a number: " + request.getVoucherId());
                }

                voucher = voucherRepository.findById(voucherId)
                        .orElseThrow(() -> new NotFoundException("Voucher not found: " + voucherId));

                // Tạo ValidateVoucherRequest với bookingAmount = tổng tiền trước khi giảm
                ValidateVoucherRequest validateRequest = ValidateVoucherRequest.builder()
                        .voucherCode(voucher.getCode())
                        .bookingId(booking.getId())
                        .bookingAmount(totalPrice)
                        .build();

                var validationResult = voucherService.validateVoucher(validateRequest, userId);
                discount = validationResult.getDiscountAmount() != null ? validationResult.getDiscountAmount() : BigDecimal.ZERO;

                // Áp dụng voucher cho booking
                booking.setVoucher(voucher);
                booking.setDiscountAmount(discount);
                booking.setFinalAmount(totalPrice.subtract(discount).max(BigDecimal.ZERO));

            } else {
                booking.setDiscountAmount(BigDecimal.ZERO);
                booking.setFinalAmount(totalPrice.setScale(2, RoundingMode.HALF_UP));
            }

// Lưu booking sau khi áp dụng voucher/không có voucher
            bookingRepository.save(booking);

            // 9. Tạo bookingSeats
            Map<Long, Seat> seatMap = seatRepository.findAllById(
                    seatInfos.stream().map(SeatInfo::getSeatId).toList()
            ).stream().collect(Collectors.toMap(Seat::getId, Function.identity()));

            List<BookingSeat> bookingSeats = new ArrayList<>();
            for (SeatInfo info : seatInfos) {
                Seat seat = seatMap.get(info.getSeatId());

                BigDecimal multiplier = info.getSeatType() == SeatType.VIP ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
                BigDecimal price = showtime.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

                bookingSeats.add(BookingSeat.builder()
                        .booking(booking)
                        .seat(seat)
                        .price(price)
                        .seatNumber(seat.getSeatNumber())
                        .rowLabel(seat.getRowLabel())
                        .seatType(seat.getSeatType())
                        .build());
            }
            bookingSeatRepository.saveAll(bookingSeats);
            booking.setBookingSeats(bookingSeats);

            // 10. Consume Redis hold
            seatClient.consumeHoldToBooked(request.getShowtimeId(), request.getSeatIds());

            log.info("[BOOKING] Successfully created booking ID {} for user {}", booking.getId(), userId);

            // 11. Return response
            return bookingMapper.toResponse(booking);

        } finally {
            // 12. Release locks
            for (Long seatId : lockedSeats) {
                redisLockService.releaseSeatLock(request.getShowtimeId(), seatId);
            }
            log.debug("[BOOKING] Released {} locks", lockedSeats.size());
        }
    }


    @Transactional
    protected Booking createBookingTransaction(
            Showtime showtime,
            List<SeatInfo> seatInfos,
            BookingRequest request,
            Long userId,
            Voucher voucher,
            BigDecimal discount
    ) {
        // Check DB booked seats to prevent race condition
        List<Long> bookedSeats = bookingSeatRepository.findBookedSeatIds(
                request.getShowtimeId(),
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED),
                request.getSeatIds()
        );
        if (!bookedSeats.isEmpty()) {
            throw new ConflictException("Seats already booked: %s".formatted(bookedSeats));
        }

        var account = SecurityUtils.getCurrentUserDetails().account();

        // Build booking entity
        Booking booking = Booking.builder()
                .account(account)
                .showtime(showtime)
                .voucher(voucher)
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        Map<Long, Seat> seatMap = seatRepository.findAllById(
                seatInfos.stream().map(SeatInfo::getSeatId).toList()
        ).stream().collect(Collectors.toMap(Seat::getId, Function.identity()));

        List<BookingSeat> bookingSeats = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (SeatInfo info : seatInfos) {
            Seat seat = seatMap.get(info.getSeatId());

            BigDecimal multiplier = info.getSeatType() == SeatType.VIP ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
            BigDecimal price = showtime.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

            bookingSeats.add(BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(price)
                    .seatNumber(seat.getSeatNumber())
                    .rowLabel(seat.getRowLabel())
                    .seatType(seat.getSeatType())
                    .build());

            totalPrice = totalPrice.add(price);
        }

        // Apply discount
        BigDecimal finalAmount = totalPrice.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        booking.setTotalPrice(totalPrice);
        booking.setDiscountAmount(discount);
        booking.setFinalAmount(finalAmount);

        // Persist booking
        bookingRepository.save(booking);
        bookingSeatRepository.saveAll(bookingSeats);
        booking.setBookingSeats(bookingSeats);

        return booking;
    }

    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        throw new UnsupportedOperationException("Booking update is not supported.");
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException("Booking deletion is not supported. Use payment cancellation.");
    }

    @Override
    public BookingResponse getById(Long id) {
        Booking booking = bookingRepository.findByIdWithSeats(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        throw new UnsupportedOperationException("Pagination not yet implemented");
    }

    // ===== PRIVATE HELPERS =====
    private void validateShowTime(Showtime showtime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showtimeStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());

        if (showtimeStart.isBefore(now)) {
            throw new BadRequestException("Cannot book past showtime: %s".formatted(showtimeStart));
        }

        LocalDateTime cutoffTime = showtimeStart.minusMinutes(15);
        if (now.isAfter(cutoffTime)) {
            throw new BadRequestException("Booking closes 15 minutes before showtime. Cutoff: %s".formatted(cutoffTime));
        }
    }
    private BigDecimal calculateTotalPrice(List<SeatInfo> seatInfos, Showtime showtime) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (SeatInfo info : seatInfos) {
            BigDecimal multiplier = info.getSeatType() == SeatType.VIP ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
            BigDecimal price = showtime.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            totalPrice = totalPrice.add(price);
        }
        return totalPrice;
    }
}
