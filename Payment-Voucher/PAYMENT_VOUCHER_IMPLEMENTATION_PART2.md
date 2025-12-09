# 🛠️ Payment & Voucher Implementation Guide - Part 2

> Service Layer, Controllers, Testing & Configuration

---

## 📦 Step 6: VoucherService Implementation

### VoucherValidationResult.java (DTO)

```java
package com.trainning.movie_booking_system.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherValidationResult {
    private boolean valid;
    private String errorMessage;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Long voucherId;
    private String voucherCode;

    public static VoucherValidationResult invalid(String errorMessage) {
        return VoucherValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static VoucherValidationResult success(
            Long voucherId,
            String voucherCode,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
        return VoucherValidationResult.builder()
                .valid(true)
                .voucherId(voucherId)
                .voucherCode(voucherCode)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }
}
```

### IVoucherService.java

```java
package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.VoucherValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface IVoucherService {
    
    /**
     * 8-step validation logic
     */
    VoucherValidationResult validateVoucher(
            String voucherCode,
            Long userId,
            Long bookingId,
            BigDecimal bookingAmount
    );

    /**
     * Get public active vouchers
     */
    Page<VoucherResponse> getPublicVouchers(Pageable pageable);

    /**
     * Get user's voucher usage history
     */
    Page<VoucherUsageResponse> getUserVoucherHistory(Long userId, Pageable pageable);

    /**
     * Admin: Create new voucher
     */
    VoucherResponse createVoucher(CreateVoucherRequest request, Long adminUserId);

    /**
     * Admin: Update voucher
     */
    VoucherResponse updateVoucher(Long voucherId, CreateVoucherRequest request);

    /**
     * Admin: Deactivate voucher
     */
    void deactivateVoucher(Long voucherId);

    /**
     * Record voucher usage
     */
    void recordVoucherUsage(
            Long voucherId,
            Long userId,
            Long bookingId,
            Long paymentTransactionId,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    );

    /**
     * Return voucher (when refund)
     */
    void returnVoucher(Long bookingId);
}
```

### VoucherServiceImpl.java

```java
package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.VoucherValidationResult;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.ResourceNotFoundException;
import com.trainning.movie_booking_system.mapper.VoucherMapper;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.service.IVoucherService;
import com.trainning.movie_booking_system.utils.enums.VoucherDiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final VoucherMapper voucherMapper;

    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResult validateVoucher(
            String voucherCode,
            Long userId,
            Long bookingId,
            BigDecimal bookingAmount
    ) {
        log.info("Validating voucher: {} for user: {}, booking: {}, amount: {}",
                voucherCode, userId, bookingId, bookingAmount);

        // Step 1: Check if voucher exists
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElse(null);
        if (voucher == null) {
            return VoucherValidationResult.invalid("Voucher code không tồn tại");
        }

        // Step 2: Check voucher status
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            return VoucherValidationResult.invalid("Voucher đã bị vô hiệu hóa");
        }

        // Step 3: Check date range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom())) {
            return VoucherValidationResult.invalid("Voucher chưa có hiệu lực");
        }
        if (now.isAfter(voucher.getValidUntil())) {
            return VoucherValidationResult.invalid("Voucher đã hết hạn");
        }

        // Step 4: Check total usage limit
        if (voucher.getCurrentUsageCount() >= voucher.getTotalUsageLimit()) {
            return VoucherValidationResult.invalid("Voucher đã hết lượt sử dụng");
        }

        // Step 5: Check user usage limit
        long userUsageCount = voucherUsageRepository.countByVoucherIdAndUserId(
                voucher.getId(), userId
        );
        if (userUsageCount >= voucher.getUsagePerUser()) {
            return VoucherValidationResult.invalid(
                    "Bạn đã sử dụng hết lượt cho voucher này (tối đa " +
                            voucher.getUsagePerUser() + " lần)"
            );
        }

        // Step 6: Check minimum order amount
        if (bookingAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
            return VoucherValidationResult.invalid(
                    "Giá trị đơn hàng tối thiểu: " +
                            voucher.getMinOrderAmount().setScale(0, RoundingMode.HALF_UP) + " VND"
            );
        }

        // Step 7: Check applicable scope (movie, theater, day, time)
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Check applicable movies
        if (voucher.getApplicableMovieIds() != null && !voucher.getApplicableMovieIds().isEmpty()) {
            Long movieId = booking.getShowtime().getMovie().getId();
            if (!voucher.getApplicableMovieIds().contains(movieId)) {
                return VoucherValidationResult.invalid("Voucher không áp dụng cho phim này");
            }
        }

        // Check applicable theaters
        if (voucher.getApplicableTheaterIds() != null && !voucher.getApplicableTheaterIds().isEmpty()) {
            Long theaterId = booking.getShowtime().getTheater().getId();
            if (!voucher.getApplicableTheaterIds().contains(theaterId)) {
                return VoucherValidationResult.invalid("Voucher không áp dụng cho rạp này");
            }
        }

        // Check applicable days of week
        if (voucher.getApplicableDaysOfWeek() != null && !voucher.getApplicableDaysOfWeek().isEmpty()) {
            int dayOfWeek = booking.getShowtime().getStartTime().getDayOfWeek().getValue();
            if (!voucher.getApplicableDaysOfWeek().contains(dayOfWeek)) {
                return VoucherValidationResult.invalid("Voucher không áp dụng cho ngày này");
            }
        }

        // Check applicable time slots
        if (voucher.getApplicableTimeSlots() != null && !voucher.getApplicableTimeSlots().isEmpty()) {
            LocalTime showtimeStart = booking.getShowtime().getStartTime().toLocalTime();
            boolean inTimeSlot = voucher.getApplicableTimeSlots().stream()
                    .anyMatch(slot -> isTimeInSlot(showtimeStart, slot));
            if (!inTimeSlot) {
                return VoucherValidationResult.invalid("Voucher không áp dụng cho khung giờ này");
            }
        }

        // Step 8: Calculate discount
        BigDecimal discountAmount = calculateDiscount(voucher, bookingAmount, booking);
        BigDecimal finalAmount = bookingAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        log.info("Voucher validation successful. Discount: {}, Final: {}", discountAmount, finalAmount);

        return VoucherValidationResult.success(
                voucher.getId(),
                voucher.getCode(),
                bookingAmount,
                discountAmount,
                finalAmount
        );
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal bookingAmount, Booking booking) {
        BigDecimal discount = BigDecimal.ZERO;

        switch (voucher.getDiscountType()) {
            case PERCENTAGE:
                // Discount = amount * percentage / 100
                discount = bookingAmount
                        .multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Apply max discount cap
                if (voucher.getMaxDiscountAmount() != null &&
                        discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discount = voucher.getMaxDiscountAmount();
                }
                break;

            case FIXED_AMOUNT:
                discount = voucher.getDiscountValue();
                break;

            case FREE_TICKET:
                // Free 1 ticket (cheapest seat)
                BigDecimal cheapestSeatPrice = booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getPrice())
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                discount = cheapestSeatPrice;
                break;

            case BUY_X_GET_Y:
                // Buy X Get Y: if user buys X tickets, get Y tickets free
                int totalSeats = booking.getBookingSeats().size();
                int buyQty = voucher.getBuyQuantity();
                int getQty = voucher.getGetQuantity();

                if (totalSeats >= buyQty) {
                    // Calculate how many free tickets
                    int freeTickets = Math.min(getQty, totalSeats - buyQty);

                    // Free tickets = cheapest seats
                    discount = booking.getBookingSeats().stream()
                            .map(bs -> bs.getSeat().getPrice())
                            .sorted()
                            .limit(freeTickets)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                break;
        }

        // Ensure discount doesn't exceed booking amount
        if (discount.compareTo(bookingAmount) > 0) {
            discount = bookingAmount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isTimeInSlot(LocalTime time, String slot) {
        // Format: "10:00-12:00"
        String[] parts = slot.split("-");
        if (parts.length != 2) return false;

        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());

            return !time.isBefore(start) && !time.isAfter(end);
        } catch (Exception e) {
            log.warn("Invalid time slot format: {}", slot);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getPublicVouchers(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Voucher> vouchers = voucherRepository.findActivePublicVouchers(
                VoucherStatus.ACTIVE,
                now,
                pageable
        );
        return vouchers.map(voucherMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherUsageResponse> getUserVoucherHistory(Long userId, Pageable pageable) {
        Page<VoucherUsage> usages = voucherUsageRepository.findByUserId(userId, pageable);
        return usages.map(voucherMapper::toUsageResponse);
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request, Long adminUserId) {
        // Validate unique code
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Voucher code đã tồn tại");
        }

        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setCreatedBy(adminUserId);
        voucher.setCurrentUsageCount(0);

        Voucher saved = voucherRepository.save(voucher);
        log.info("Created voucher: {} by admin: {}", saved.getCode(), adminUserId);

        return voucherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long voucherId, CreateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // Update fields
        voucherMapper.updateEntity(request, voucher);

        Voucher updated = voucherRepository.save(voucher);
        log.info("Updated voucher: {}", updated.getCode());

        return voucherMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateVoucher(Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);

        log.info("Deactivated voucher: {}", voucher.getCode());
    }

    @Override
    @Transactional
    public void recordVoucherUsage(
            Long voucherId,
            Long userId,
            Long bookingId,
            Long paymentTransactionId,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        Account user = accountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Increment voucher usage count
        voucher.incrementUsage();
        voucherRepository.save(voucher);

        // Record usage
        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(user)
                .booking(booking)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();

        voucherUsageRepository.save(usage);

        log.info("Recorded voucher usage: {} for user: {}, booking: {}",
                voucher.getCode(), userId, bookingId);
    }

    @Override
    @Transactional
    public void returnVoucher(Long bookingId) {
        VoucherUsage usage = voucherUsageRepository.findByBookingId(bookingId)
                .orElse(null);

        if (usage != null) {
            Voucher voucher = usage.getVoucher();
            voucher.decrementUsage();
            voucherRepository.save(voucher);

            voucherUsageRepository.deleteByBookingId(bookingId);

            log.info("Returned voucher: {} for booking: {}", voucher.getCode(), bookingId);
        }
    }
}
```

---

## 💳 Step 7: VNPay Integration

### VNPayConfig.java

```java
package com.trainning.movie_booking_system.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.ipn-url}")
    private String ipnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command:pay}")
    private String command;

    @Value("${vnpay.order-type:other}")
    private String orderType;

    @Value("${vnpay.locale:vn}")
    private String locale;

    @Value("${vnpay.currency:VND}")
    private String currency;
}
```

### application.yml (Add VNPay config)

```yaml
vnpay:
  tmn-code: ${VNPAY_TMN_CODE:YOUR_TMN_CODE}
  hash-secret: ${VNPAY_HASH_SECRET:YOUR_SECRET_KEY}
  url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  return-url: ${FRONTEND_URL:http://localhost:3000}/payment/vnpay-return
  ipn-url: ${BACKEND_URL:http://localhost:8080}/api/payments/vnpay/ipn
  version: 2.1.0
  command: pay
  order-type: other
  locale: vn
  currency: VND
```

### VNPayService.java

```java
package com.trainning.movie_booking_system.service.payment;

import com.trainning.movie_booking_system.config.VNPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    /**
     * Generate VNPay payment URL
     */
    public String createPaymentUrl(
            String orderId,
            long amount,
            String orderInfo,
            String ipAddress
    ) throws UnsupportedEncodingException {
        
        Map<String, String> vnpParams = new TreeMap<>();

        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay requires amount * 100
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrency());
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Timestamps
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(new Date());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Expiry time (15 minutes)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        calendar.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Build sign data
        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            signDataBuilder.append("=");
            signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            signDataBuilder.append("&");
        }
        signDataBuilder.deleteCharAt(signDataBuilder.length() - 1); // Remove last &

        String signData = signDataBuilder.toString();
        String signed = generateHMAC(signData, vnPayConfig.getHashSecret());
        vnpParams.put("vnp_SecureHash", signed);

        // Build final URL
        StringBuilder urlBuilder = new StringBuilder(vnPayConfig.getVnpUrl());
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            urlBuilder.append("&");
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1); // Remove last &

        String paymentUrl = urlBuilder.toString();
        log.info("Generated VNPay payment URL for order: {}", orderId);

        return paymentUrl;
    }

    /**
     * Verify VNPay callback signature (IPN)
     */
    public boolean verifyPaymentSignature(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            log.warn("Missing vnp_SecureHash in callback");
            return false;
        }

        // Remove signature from params
        Map<String, String> paramsToVerify = new TreeMap<>(params);
        paramsToVerify.remove("vnp_SecureHash");
        paramsToVerify.remove("vnp_SecureHashType");

        // Build sign data
        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : paramsToVerify.entrySet()) {
            try {
                signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                signDataBuilder.append("=");
                signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                signDataBuilder.append("&");
            } catch (UnsupportedEncodingException e) {
                log.error("Error encoding params", e);
                return false;
            }
        }
        signDataBuilder.deleteCharAt(signDataBuilder.length() - 1);

        String signData = signDataBuilder.toString();
        String calculatedHash = generateHMAC(signData, vnPayConfig.getHashSecret());

        boolean valid = calculatedHash.equalsIgnoreCase(vnpSecureHash);
        
        if (!valid) {
            log.warn("Invalid VNPay signature. Expected: {}, Got: {}", calculatedHash, vnpSecureHash);
        }

        return valid;
    }

    /**
     * Generate HMAC-SHA512 signature
     */
    private String generateHMAC(String data, String secretKey) {
        try {
            Mac sha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            sha512.init(secretKeySpec);
            byte[] hash = sha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error generating HMAC", e);
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Parse VNPay response
     */
    public Map<String, String> parseVNPayResponse(String queryString) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        
        return params;
    }
}
```

---

## 🌐 Step 8: Stripe Integration

### StripeConfig.java

```java
package com.trainning.movie_booking_system.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
```

### application.yml (Add Stripe config)

```yaml
stripe:
  api-key: ${STRIPE_API_KEY:sk_test_YOUR_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:whsec_YOUR_WEBHOOK_SECRET}
  success-url: ${FRONTEND_URL:http://localhost:3000}/payment/success?session_id={CHECKOUT_SESSION_ID}
  cancel-url: ${FRONTEND_URL:http://localhost:3000}/payment/cancel
```

### StripeService.java

```java
package com.trainning.movie_booking_system.service.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.trainning.movie_booking_system.config.StripeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;

    /**
     * Create Stripe checkout session
     */
    public Session createCheckoutSession(
            String orderId,
            BigDecimal amount,
            String orderDescription,
            String customerEmail
    ) throws StripeException {

        // Convert VND to USD (approximate rate: 1 USD = 24,000 VND)
        // Stripe requires minimum 50 cents
        long amountInCents = amount
                .divide(BigDecimal.valueOf(24000), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        if (amountInCents < 50) {
            amountInCents = 50; // Minimum 50 cents
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeConfig.getSuccessUrl())
                .setCancelUrl(stripeConfig.getCancelUrl())
                .setCustomerEmail(customerEmail)
                .setClientReferenceId(orderId)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Movie Tickets - " + orderId)
                                                                .setDescription(orderDescription)
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("Created Stripe checkout session: {} for order: {}", session.getId(), orderId);

        return session;
    }

    /**
     * Verify Stripe webhook signature
     */
    public Event verifyWebhookSignature(String payload, String sigHeader) 
            throws SignatureVerificationException {
        
        Event event = Webhook.constructEvent(
                payload,
                sigHeader,
                stripeConfig.getWebhookSecret()
        );

        log.info("Verified Stripe webhook signature for event: {}", event.getType());
        return event;
    }

    /**
     * Retrieve checkout session
     */
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
```

---

## 🔄 Step 9: Refactor PaymentService

### PaymentServiceImpl.java (Complete Refactor)

```java
package com.trainning.movie_booking_system.service.impl;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.trainning.movie_booking_system.dto.request.CreatePaymentRequest;
import com.trainning.movie_booking_system.dto.response.PaymentResponse;
import com.trainning.movie_booking_system.dto.response.VoucherValidationResult;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ResourceNotFoundException;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.service.IEmailService;
import com.trainning.movie_booking_system.service.IPaymentService;
import com.trainning.movie_booking_system.service.IVoucherService;
import com.trainning.movie_booking_system.service.payment.StripeService;
import com.trainning.movie_booking_system.service.payment.VNPayService;
import com.trainning.movie_booking_system.utils.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;
    private final VNPayService vnPayService;
    private final StripeService stripeService;
    private final IVoucherService voucherService;
    private final IEmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final PaymentWebhookLogRepository webhookLogRepository;

    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final int IDEMPOTENCY_TTL_HOURS = 24;

    @Override
    @Transactional
    public PaymentResponse createPaymentUrl(
            CreatePaymentRequest request,
            String ipAddress,
            Long userId
    ) {
        // Validate booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getAccount().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized to pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking is not in PENDING status");
        }

        // Check if payment already exists
        PaymentTransaction existingPayment = paymentTransactionRepository
                .findByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCESS)
                .orElse(null);

        if (existingPayment != null) {
            throw new BadRequestException("Booking already paid");
        }

        // Calculate amount
        BigDecimal originalAmount = booking.getTotalPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = originalAmount;
        Long voucherId = null;

        // Apply voucher if provided
        if (request.getVoucherCode() != null && !request.getVoucherCode().isEmpty()) {
            VoucherValidationResult validationResult = voucherService.validateVoucher(
                    request.getVoucherCode(),
                    userId,
                    booking.getId(),
                    originalAmount
            );

            if (!validationResult.isValid()) {
                throw new BadRequestException(validationResult.getErrorMessage());
            }

            discountAmount = validationResult.getDiscountAmount();
            finalAmount = validationResult.getFinalAmount();
            voucherId = validationResult.getVoucherId();
        }

        // Generate unique transaction ID
        String transactionId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // Create payment transaction
        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .booking(booking)
                .gatewayType(request.getGatewayType())
                .transactionId(transactionId)
                .amount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(PaymentStatus.PENDING)
                .ipAddress(ipAddress)
                .build();

        if (voucherId != null) {
            Voucher voucher = new Voucher();
            voucher.setId(voucherId);
            paymentTransaction.setVoucher(voucher);
        }

        paymentTransactionRepository.save(paymentTransaction);

        // Generate payment URL based on gateway
        String paymentUrl;
        try {
            if (request.getGatewayType() == PaymentGateway.VNPAY) {
                paymentUrl = vnPayService.createPaymentUrl(
                        transactionId,
                        finalAmount.longValue(),
                        "Thanh toán vé xem phim - " + booking.getId(),
                        ipAddress
                );
            } else if (request.getGatewayType() == PaymentGateway.STRIPE) {
                Session session = stripeService.createCheckoutSession(
                        transactionId,
                        finalAmount,
                        "Movie tickets for " + booking.getShowtime().getMovie().getTitle(),
                        booking.getAccount().getEmail()
                );
                paymentUrl = session.getUrl();
                paymentTransaction.setGatewayOrderId(session.getId());
                paymentTransactionRepository.save(paymentTransaction);
            } else {
                throw new BadRequestException("Unsupported payment gateway");
            }
        } catch (Exception e) {
            log.error("Error creating payment URL", e);
            paymentTransaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(paymentTransaction);
            throw new RuntimeException("Error creating payment URL: " + e.getMessage());
        }

        log.info("Created payment URL for booking: {}, transaction: {}", booking.getId(), transactionId);

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .paymentUrl(paymentUrl)
                .amount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .gatewayType(request.getGatewayType())
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Override
    @Transactional
    public void handleVNPayCallback(Map<String, String> params) {
        // Step 1: Verify signature
        boolean signatureValid = vnPayService.verifyPaymentSignature(params);

        // Step 2: Log webhook
        PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                .gatewayType(PaymentGateway.VNPAY)
                .eventType("payment_callback")
                .requestBody(new HashMap<>(params))
                .signature(params.get("vnp_SecureHash"))
                .signatureValid(signatureValid)
                .build();

        if (!signatureValid) {
            webhookLog.setProcessingError("Invalid signature");
            webhookLogRepository.save(webhookLog);
            throw new BadRequestException("Invalid signature");
        }

        // Step 3: Extract data
        String transactionId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String gatewayTransactionId = params.get("vnp_TransactionNo");

        // Step 4: Idempotency check
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + transactionId;
        Boolean processed = redisTemplate.opsForValue().setIfAbsent(
                idempotencyKey,
                "1",
                IDEMPOTENCY_TTL_HOURS,
                TimeUnit.HOURS
        );

        if (Boolean.FALSE.equals(processed)) {
            log.warn("Duplicate VNPay callback for transaction: {}", transactionId);
            webhookLog.setProcessed(true);
            webhookLog.setProcessingError("Duplicate callback");
            webhookLogRepository.save(webhookLog);
            return;
        }

        // Step 5: Find payment transaction
        PaymentTransaction payment = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        webhookLog.setPaymentTransaction(payment);

        // Step 6: Process payment result
        try {
            if ("00".equals(responseCode)) {
                // Payment SUCCESS
                processSuccessfulPayment(payment, gatewayTransactionId, params);
            } else {
                // Payment FAILED
                processFailedPayment(payment, responseCode, params);
            }

            webhookLog.setProcessed(true);
            webhookLog.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            webhookLog.setProcessingError(e.getMessage());
        }

        webhookLogRepository.save(webhookLog);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        // Step 1: Verify signature
        Event event;
        try {
            event = stripeService.verifyWebhookSignature(payload, sigHeader);
        } catch (Exception e) {
            log.error("Invalid Stripe webhook signature", e);
            
            PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                    .gatewayType(PaymentGateway.STRIPE)
                    .signature(sigHeader)
                    .signatureValid(false)
                    .processingError("Invalid signature: " + e.getMessage())
                    .build();
            webhookLogRepository.save(webhookLog);
            
            throw new BadRequestException("Invalid signature");
        }

        // Step 2: Log webhook
        PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                .gatewayType(PaymentGateway.STRIPE)
                .eventType(event.getType())
                .signature(sigHeader)
                .signatureValid(true)
                .build();

        // Step 3: Handle event
        try {
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Session not found in event"));

                String transactionId = session.getClientReferenceId();

                // Idempotency check
                String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + transactionId;
                Boolean processed = redisTemplate.opsForValue().setIfAbsent(
                        idempotencyKey,
                        "1",
                        IDEMPOTENCY_TTL_HOURS,
                        TimeUnit.HOURS
                );

                if (Boolean.FALSE.equals(processed)) {
                    log.warn("Duplicate Stripe webhook for transaction: {}", transactionId);
                    webhookLog.setProcessed(true);
                    webhookLog.setProcessingError("Duplicate webhook");
                    webhookLogRepository.save(webhookLog);
                    return;
                }

                PaymentTransaction payment = paymentTransactionRepository
                        .findByTransactionId(transactionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

                webhookLog.setPaymentTransaction(payment);

                // Process successful payment
                Map<String, String> metadata = new HashMap<>();
                metadata.put("session_id", session.getId());
                metadata.put("payment_status", session.getPaymentStatus());

                processSuccessfulPayment(payment, session.getPaymentIntent(), metadata);

                webhookLog.setProcessed(true);
                webhookLog.setProcessedAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            webhookLog.setProcessingError(e.getMessage());
        }

        webhookLogRepository.save(webhookLog);
    }

    private void processSuccessfulPayment(
            PaymentTransaction payment,
            String gatewayTransactionId,
            Map<String, ?> gatewayResponse
    ) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already processed: {}", payment.getTransactionId());
            return;
        }

        // Update payment transaction
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayOrderId(gatewayTransactionId);
        payment.setGatewayResponse((Map<String, Object>) gatewayResponse);
        payment.setCompletedAt(LocalDateTime.now());
        paymentTransactionRepository.save(payment);

        // Update booking status
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);

        // Record voucher usage if applied
        if (payment.getVoucher() != null) {
            voucherService.recordVoucherUsage(
                    payment.getVoucher().getId(),
                    booking.getAccount().getId(),
                    booking.getId(),
                    payment.getId(),
                    payment.getAmount(),
                    payment.getDiscountAmount(),
                    payment.getFinalAmount()
            );
        }

        // Send confirmation email with QR code
        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            log.error("Error sending confirmation email", e);
        }

        log.info("Payment SUCCESS: transaction={}, booking={}", payment.getTransactionId(), booking.getId());
    }

    private void processFailedPayment(
            PaymentTransaction payment,
            String errorCode,
            Map<String, String> gatewayResponse
    ) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setGatewayResponse((Map<String, Object>) (Object) gatewayResponse);
        payment.setCompletedAt(LocalDateTime.now());
        paymentTransactionRepository.save(payment);

        log.warn("Payment FAILED: transaction={}, errorCode={}", payment.getTransactionId(), errorCode);
    }
}
```

---

Bạn muốn tôi tiếp tục với **Step 10 (Controllers & DTOs)** và **Step 11 (Testing Guide)** không? 🚀

