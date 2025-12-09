package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;
import com.trainning.movie_booking_system.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * PaymentController - Xử lý toàn bộ luồng thanh toán cho booking.
 *
 * <p>Hỗ trợ:</p>
 * <ul>
 *     <li>Tạo URL thanh toán (VNPay)</li>
 *     <li>Xử lý Return URL từ frontend</li>
 *     <li>Nhận IPN (webhook) từ gateway</li>
 *     <li>Verify trạng thái payment</li>
 *     <li>Cancel payment / release ghế</li>
 * </ul>
 *
 * <p>⚠️ Chú ý:</p>
 * <ul>
 *     <li>Webhook phải verify signature trước khi cập nhật trạng thái</li>
 *     <li>Frontend phải gọi verify endpoint để confirm kết quả thanh toán</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo payment URL để redirect user sang gateway.
     *
     * @param bookingId ID booking cần thanh toán
     * @return URL payment để frontend redirect user
     */
    @PostMapping("/bookings/{bookingId}/payment")
    public ResponseEntity<BaseResponse<String>> createPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT] Create payment for booking {}", bookingId);

        String paymentUrl = paymentService.createPaymentUrl(bookingId);
        return ResponseEntity.ok(BaseResponse.success(
                paymentUrl,
                "Redirect to payment gateway. Complete payment within 15 minutes."
        ));
    }

    /**
     * VNPay Return URL (LOCAL TESTING hoặc frontend redirect)
     *
     * <p>Backend sẽ verify signature, cập nhật transaction & booking.</p>
     *
     * @param request HttpServletRequest chứa params từ VNPay
     * @return PaymentResponse JSON
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<BaseResponse<PaymentResponse>> handleVNPayReturn(HttpServletRequest request) {
        log.info("[PAYMENT] VNPay return callback received");

        PaymentResponse response = paymentService.handleVNPayReturn(request);
        return ResponseEntity.ok(BaseResponse.success(
                response,
                "Payment processed. Check booking status for details."
        ));
    }

    /**
     * VNPay verify endpoint (PRODUCTION MODE) - frontend gọi
     *
     * <p>Frontend nhận params từ VNPay redirect, gửi sang đây để verify và update booking.</p>
     *
     * @param request HttpServletRequest chứa params từ frontend
     * @return PaymentResponse JSON
     */
    @PostMapping("/vnpay/verify")
    public ResponseEntity<BaseResponse<PaymentResponse>> verifyVNPayPayment(HttpServletRequest request) {
        log.info("[PAYMENT] VNPay verify called from frontend");

        PaymentResponse response = paymentService.handleVNPayReturn(request);
        return ResponseEntity.ok(BaseResponse.success(
                response,
                "Payment verification completed"
        ));
    }

    /**
     * Payment Gateway Webhook / IPN endpoint
     *
     * <p>Gateway sẽ gọi endpoint này để thông báo server về payment thành công / failed.</p>
     *
     * <p>⚠️ CRITICAL: Phải verify signature trước khi update booking & transaction.</p>
     *
     * @param request PaymentRequest chứa dữ liệu callback
     * @return PaymentResponse JSON
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<BaseResponse<PaymentResponse>> handleVNPayIPN(
            @RequestBody @Valid PaymentRequest request) {
        log.info("[PAYMENT] VNPay IPN received for booking {}", request.getBookingId());

        PaymentResponse response = paymentService.handlePaymentCallback(request);
        return ResponseEntity.ok(BaseResponse.success(response, "IPN processed"));
    }

    /**
     * Kiểm tra trạng thái payment của booking
     *
     * @param bookingId ID booking
     * @return Trạng thái payment hiện tại
     */
    @GetMapping("/bookings/{bookingId}/payment-status")
    public ResponseEntity<BaseResponse<String>> verifyPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT] Verify payment status for booking {}", bookingId);

        String status = paymentService.verifyPaymentStatus(bookingId);
        return ResponseEntity.ok(BaseResponse.success(status, "Payment status: " + status));
    }

    /**
     * Hủy payment (user cancel hoặc timeout)
     *
     * <p>Update booking status và release ghế giữ.</p>
     *
     * @param bookingId ID booking cần cancel
     * @return Success message
     */
    @DeleteMapping("/bookings/{bookingId}/payment")
    public ResponseEntity<BaseResponse<Void>> cancelPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT] Cancel payment for booking {}", bookingId);

        paymentService.cancelPayment(bookingId);
        return ResponseEntity.ok(BaseResponse.success(null, "Payment cancelled successfully. Seats are now available for other users."));
    }
}
