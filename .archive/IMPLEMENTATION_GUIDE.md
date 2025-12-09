# 🛠️ HƯỚNG DẪN FIX TỪNG BƯỚC - PAYMENT & BOOKING ISSUES

> **Branch:** feature/payment-voucher-integration  
> **Date:** November 11, 2025  
> **Estimated Total Time:** ~14 hours (2 days)

---

## 📋 OVERVIEW - CÁC BƯỚC CẦN LÀM

| Step | Issue | Priority | Time | Files to Edit |
|------|-------|----------|------|---------------|
| ✅ **1** | Add showtime validation | 🔴 CRITICAL | 30min | `BookingServiceImpl.java` |
| ✅ **2** | Add idempotency check | 🔴 CRITICAL | 2h | `PaymentServiceImpl.java`, `PaymentTransactionRepository.java` |
| ✅ **3** | Create expiration cron job | 🔴 CRITICAL | 3h | `BookingExpirationService.java` (NEW), `BookingRepository.java` |
| ✅ **4** | Add VNPay IPN webhook | 🔴 CRITICAL | 3h | `PaymentController.java` |
| ✅ **5** | Add payment logging | 🔴 CRITICAL | 2h | `PaymentServiceImpl.java` |
| ✅ **6** | Fix seat release logic | 🔴 CRITICAL | 2h | `PaymentServiceImpl.java` |
| ⚠️ **7** | Add database indexes | 🟡 MAJOR | 1h | `Booking.java` |
| ⚠️ **8** | Implement pagination | 🟡 MAJOR | 2h | `BookingServiceImpl.java` |

**Total:** ~15.5 hours

---

## 🚀 STEP 1: Add Showtime Validation (30 minutes)

### **Vấn đề:**
User có thể book ghế cho suất chiếu đã qua hoặc sắp bắt đầu (< 15 phút).

### **Fix Location:**
`src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java`

### **Code Changes:**

#### **1.1. Thêm constants class (tạo file mới)**

```java
// File: src/main/java/com/trainning/movie_booking_system/untils/constants/BookingConstants.java
package com.trainning.movie_booking_system.untils.constants;

import java.math.BigDecimal;

public class BookingConstants {
    
    // Booking timeouts
    public static final int SEAT_HOLD_TTL_SECONDS = 120;  // 2 minutes
    public static final int BOOKING_PAYMENT_TIMEOUT_MINUTES = 15;
    
    // Cutoff times
    public static final int BOOKING_CUTOFF_MINUTES = 15;  // Cannot book < 15 min before showtime
    
    // Seat lock
    public static final int SEAT_LOCK_TIMEOUT_SECONDS = 30;
    
    // Pricing
    public static final BigDecimal VIP_PRICE_MULTIPLIER = new BigDecimal("1.3");
    public static final BigDecimal STANDARD_PRICE_MULTIPLIER = BigDecimal.ONE;
    
    private BookingConstants() {
        // Private constructor to prevent instantiation
    }
}
```

#### **1.2. Update BookingServiceImpl**

Tìm method `create()` và thêm validation:

```java
@Override
public BookingResponse create(BookingRequest request) {
    log.info("[BOOKING] Create booking request: {}", request);

    var currentUser = SecurityUtils.getCurrentUserDetails();
    Long userId = currentUser.getAccount().getId();

    // ===== 1. Validate input =====
    if (CollectionUtils.isEmpty(request.getSeatIds())) {
        throw new BadRequestException("Seat list must not be empty");
    }

    // ===== 2. Validate showtime exists & not started =====
    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
            .orElseThrow(() -> new NotFoundException("Showtime not found with ID: " + request.getShowtimeId()));
    
    // ✅ NEW: Validate showtime time
    LocalDateTime now = LocalDateTime.now();
    
    if (showtime.getStartTime().isBefore(now)) {
        throw new BadRequestException(
            String.format("Cannot book for showtime that has already started. Showtime started at: %s", 
                showtime.getStartTime())
        );
    }
    
    LocalDateTime cutoffTime = showtime.getStartTime().minusMinutes(BookingConstants.BOOKING_CUTOFF_MINUTES);
    if (now.isAfter(cutoffTime)) {
        throw new BadRequestException(
            String.format("Booking closes %d minutes before showtime. Cutoff time: %s", 
                BookingConstants.BOOKING_CUTOFF_MINUTES, cutoffTime)
        );
    }

    // ===== 3. Verify seats are held by current user (pre-check) =====
    // ... rest of existing code ...
}
```

### **Testing:**

```bash
# Test 1: Book for showtime that already started
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "showtimeId": 1,  # Showtime với startTime < now
    "seatIds": [1, 2, 3]
  }'
# Expected: 400 Bad Request - "Cannot book for showtime that has already started"

# Test 2: Book < 15 minutes before showtime
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "showtimeId": 2,  # Showtime với startTime = now + 10 minutes
    "seatIds": [1, 2, 3]
  }'
# Expected: 400 Bad Request - "Booking closes 15 minutes before showtime"

# Test 3: Valid booking (> 15 minutes before)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "showtimeId": 3,  # Showtime với startTime = now + 60 minutes
    "seatIds": [1, 2, 3]
  }'
# Expected: 201 Created
```

### **Commit Message:**
```
fix(booking): add showtime validation to prevent booking past/imminent shows

- Validate showtime has not started
- Add 15-minute cutoff before showtime
- Create BookingConstants for magic numbers
- Add comprehensive validation error messages

Fixes: #6
```

---

## 🚀 STEP 2: Add Idempotency Check (2 hours)

### **Vấn đề:**
VNPay callback có thể được gọi nhiều lần (user refresh, VNPay retry), dẫn đến duplicate processing.

### **Fix Locations:**
1. `PaymentTransactionRepository.java` - Add pessimistic lock query
2. `PaymentServiceImpl.java` - Add idempotency check

### **Code Changes:**

#### **2.1. Update PaymentTransactionRepository**

Thêm method với `@Lock`:

```java
// File: src/main/java/com/trainning/movie_booking_system/repository/PaymentTransactionRepository.java

package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.PaymentTransaction;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    // ✅ NEW: Pessimistic lock for idempotency
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.transactionId = :txnId")
    Optional<PaymentTransaction> findByTransactionIdForUpdate(@Param("txnId") String txnId);

    List<PaymentTransaction> findByBookingId(Long bookingId);

    // ✅ NEW: Find by booking and status
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.booking.id = :bookingId AND pt.status = :status")
    List<PaymentTransaction> findByBookingIdAndStatus(
            @Param("bookingId") Long bookingId,
            @Param("status") PaymentStatus status
    );

    boolean existsByTransactionId(String transactionId);
}
```

#### **2.2. Update PaymentServiceImpl**

Thay thế toàn bộ method `handleVNPayReturn()`:

```java
// File: src/main/java/com/trainning/movie_booking_system/service/impl/PaymentServiceImpl.java

@Override
@Transactional(isolation = Isolation.SERIALIZABLE)
public PaymentResponse handleVNPayReturn(HttpServletRequest request) {
    log.info("[PAYMENT] Processing VNPay return callback");

    // STEP 1: Verify signature FIRST
    int paymentStatus = vnPayService.orderReturn(request);
    
    String vnpTxnRef = request.getParameter("vnp_TxnRef");
    String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
    String vnpAmount = request.getParameter("vnp_Amount");
    String vnpBankCode = request.getParameter("vnp_BankCode");
    
    log.info("[PAYMENT] VNPay callback - TxnRef: {}, Status: {}, Amount: {}", 
        vnpTxnRef, paymentStatus, vnpAmount);

    if (paymentStatus == -1) {
        log.error("[PAYMENT] ⚠️ SECURITY: Invalid VNPay signature for transaction: {}", vnpTxnRef);
        throw new BadRequestException("Invalid payment signature");
    }

    // STEP 2: Acquire distributed lock (prevent concurrent processing)
    String lockKey = "payment:lock:" + vnpTxnRef;
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(
        lockKey, "locked", 30, TimeUnit.SECONDS
    );
    
    if (Boolean.FALSE.equals(locked)) {
        log.warn("[PAYMENT] Concurrent processing detected for txn {}, aborting duplicate", vnpTxnRef);
        throw new ConflictException("Payment is being processed, please wait");
    }
    
    try {
        // STEP 3: Load transaction with pessimistic lock
        PaymentTransaction transaction = paymentTransactionRepository
            .findByTransactionIdForUpdate(vnpTxnRef)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + vnpTxnRef));

        Booking booking = transaction.getBooking();

        // STEP 4: IDEMPOTENCY CHECK
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            log.warn("[PAYMENT] ⚠️ Duplicate callback for transaction {} (current status: {})", 
                vnpTxnRef, transaction.getStatus());
            
            return PaymentResponse.builder()
                .bookingId(booking.getId())
                .status(transaction.getStatus().name())
                .message("Transaction already processed (idempotent response)")
                .build();
        }

        // STEP 5: Business validations
        LocalDateTime now = LocalDateTime.now();
        
        // Check booking not expired
        if (booking.getExpiresAt() != null && now.isAfter(booking.getExpiresAt())) {
            log.warn("[PAYMENT] ⚠️ Booking {} expired at {}, initiating refund", 
                booking.getId(), booking.getExpiresAt());
            
            transaction.setStatus(PaymentStatus.REFUND_PENDING);
            transaction.setCompletedAt(now);
            paymentTransactionRepository.save(transaction);
            
            // TODO: Call VNPay refund API
            
            return PaymentResponse.builder()
                .bookingId(booking.getId())
                .status("REFUND_PENDING")
                .message("Booking expired, refund will be processed within 24h")
                .build();
        }
        
        // Check seats still available
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
        
        List<Long> bookedSeats = bookingSeatRepository.findBookedSeatIds(
            booking.getShowtime().getId(),
            List.of(BookingStatus.CONFIRMED),
            seatIds
        );
        
        if (!bookedSeats.isEmpty()) {
            log.warn("[PAYMENT] ⚠️ Seats {} already booked by others, initiating refund", bookedSeats);
            
            transaction.setStatus(PaymentStatus.REFUND_PENDING);
            transaction.setCompletedAt(now);
            paymentTransactionRepository.save(transaction);
            
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // TODO: Call VNPay refund API
            
            return PaymentResponse.builder()
                .bookingId(booking.getId())
                .status("REFUND_PENDING")
                .message("Seats no longer available, refund will be processed")
                .build();
        }

        // STEP 6: Process payment based on status
        if (paymentStatus == 1) {
            // ✅ SUCCESS
            log.info("[PAYMENT] ✅ Payment SUCCESS for booking {}", booking.getId());

            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setGatewayOrderId(vnpTransactionNo);
            transaction.setPaymentMethod(vnpBankCode);
            transaction.setCompletedAt(now);
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // Consume holds
            seatDomainService.consumeHoldToBooked(booking.getShowtime().getId(), seatIds);

            // TODO: Send email confirmation
            log.info("[PAYMENT] TODO: Send booking confirmation email to {}", 
                booking.getAccount().getEmail());

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("SUCCESS")
                    .message("Payment completed successfully")
                    .build();

        } else {
            // ❌ FAILED
            log.warn("[PAYMENT] ❌ Payment FAILED for booking {}", booking.getId());

            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setCompletedAt(now);
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Release seats
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("FAILED")
                    .message("Payment failed or cancelled")
                    .build();
        }
        
    } finally {
        // STEP 7: Always release lock
        redisTemplate.delete(lockKey);
        log.debug("[PAYMENT] Released lock for transaction {}", vnpTxnRef);
    }
}
```

#### **2.3. Add RedisTemplate dependency (if not exists)**

Check `PaymentServiceImpl` constructor:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final SeatDomainService seatDomainService;
    private final VnPayService vnPayService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingSeatRepository bookingSeatRepository; // ✅ Add if missing
    private final StringRedisTemplate redisTemplate; // ✅ Add this
}
```

### **Testing:**

```bash
# Test 1: Normal payment (first time)
curl -X GET "http://localhost:8080/api/v1/payments/vnpay/callback?vnp_TxnRef=TXN_123&vnp_TransactionStatus=00&vnp_SecureHash=..."
# Expected: 200 OK - Payment SUCCESS

# Test 2: Duplicate callback (user refresh)
curl -X GET "http://localhost:8080/api/v1/payments/vnpay/callback?vnp_TxnRef=TXN_123&vnp_TransactionStatus=00&vnp_SecureHash=..."
# Expected: 200 OK - "Transaction already processed (idempotent response)"
# Verify: Booking chỉ confirm 1 lần, không duplicate

# Test 3: Expired booking payment
# 1. Create booking
# 2. Wait > 15 minutes (or manually update expires_at in DB)
# 3. Call callback
curl -X GET "http://localhost:8080/api/v1/payments/vnpay/callback?vnp_TxnRef=TXN_456&..."
# Expected: 200 OK - "REFUND_PENDING - Booking expired"
```

### **Commit Message:**
```
fix(payment): add idempotency check to prevent duplicate payment processing

- Add pessimistic lock query in PaymentTransactionRepository
- Implement distributed Redis lock for concurrent requests
- Add idempotency guard checking transaction status
- Handle expired bookings with refund flow
- Handle seat conflicts with refund flow

Fixes: #1
```

---

## 🚀 STEP 3: Create Booking Expiration Cron Job (3 hours)

### **Vấn đề:**
Bookings PENDING_PAYMENT không tự động expire → ghế bị lock mãi mãi.

### **Fix Locations:**
1. Create `BookingExpirationService.java` (NEW)
2. Update `BookingRepository.java` - Add query method
3. Update `Booking.java` - Add `expiresAt` field & index

### **Code Changes:**

#### **3.1. Update Booking entity - Add expiresAt field**

```java
// File: src/main/java/com/trainning/movie_booking_system/entity/Booking.java

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_account", columnList = "account_id"),
                @Index(name = "idx_booking_showtime", columnList = "showtime_id"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_expires_at", columnList = "expires_at") // ✅ NEW
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    // ... existing fields ...

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;
    
    // ✅ NEW: Expiration time for PENDING_PAYMENT bookings
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ... rest of code ...

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        // ✅ NEW: Set expiration time (15 minutes from creation)
        if (status == BookingStatus.PENDING_PAYMENT) {
            expiresAt = bookingDate.plusMinutes(BookingConstants.BOOKING_PAYMENT_TIMEOUT_MINUTES);
        }
    }
}
```

#### **3.2. Update BookingRepository - Add query**

```java
// File: src/main/java/com/trainning/movie_booking_system/repository/BookingRepository.java

package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ✅ NEW: Find expired bookings for cron job
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :expirationTime")
    List<Booking> findByStatusAndExpiresAtBefore(
            @Param("status") BookingStatus status,
            @Param("expirationTime") LocalDateTime expirationTime
    );

    // Existing methods...
}
```

#### **3.3. Create BookingExpirationService (NEW FILE)**

```java
// File: src/main/java/com/trainning/movie_booking_system/service/impl/BookingExpirationService.java

package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.PaymentTransaction;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.PaymentTransactionRepository;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service để tự động expire các bookings PENDING_PAYMENT đã quá hạn
 * Chạy mỗi 5 phút
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SeatDomainService seatDomainService;

    /**
     * Expire bookings that are PENDING_PAYMENT and past expiration time
     * Runs every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @Transactional
    public void expireBookings() {
        log.info("[EXPIRATION] Starting expiration job");

        LocalDateTime now = LocalDateTime.now();

        // Find expired bookings
        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);

        if (expiredBookings.isEmpty()) {
            log.info("[EXPIRATION] No expired bookings found");
            return;
        }

        log.info("[EXPIRATION] Found {} expired bookings", expiredBookings.size());

        int successCount = 0;
        int failCount = 0;

        for (Booking booking : expiredBookings) {
            try {
                expireBooking(booking);
                successCount++;
            } catch (Exception e) {
                log.error("[EXPIRATION] Failed to expire booking {}", booking.getId(), e);
                failCount++;
            }
        }

        log.info("[EXPIRATION] Expiration job completed. Success: {}, Failed: {}",
                successCount, failCount);
    }

    /**
     * Expire a single booking
     */
    private void expireBooking(Booking booking) {
        log.info("[EXPIRATION] Expiring booking {} (created: {}, expired: {})",
                booking.getId(), booking.getBookingDate(), booking.getExpiresAt());

        // Update booking status
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        // Cancel pending payment transactions
        List<PaymentTransaction> pendingTxns = paymentTransactionRepository
                .findByBookingIdAndStatus(booking.getId(), PaymentStatus.PENDING);

        for (PaymentTransaction txn : pendingTxns) {
            txn.setStatus(PaymentStatus.EXPIRED);
            txn.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(txn);
            log.debug("[EXPIRATION] Cancelled transaction {}", txn.getTransactionId());
        }

        // Release seats (cleanup Redis holds if any)
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();

        try {
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);
            log.debug("[EXPIRATION] Released holds for {} seats", seatIds.size());
        } catch (Exception e) {
            log.warn("[EXPIRATION] Failed to release holds for booking {} (may already released)",
                    booking.getId());
        }

        // TODO: Send notification email
        // emailService.sendBookingExpiredNotification(booking);

        log.info("[EXPIRATION] ✅ Booking {} expired successfully", booking.getId());
    }
}
```

#### **3.4. Enable Scheduling in Application**

Check `MovieBookingSystemApplication.java`:

```java
// File: src/main/java/com/trainning/movie_booking_system/MovieBookingSystemApplication.java

package com.trainning.movie_booking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ Add this

@SpringBootApplication
@EnableScheduling // ✅ Add this to enable @Scheduled
public class MovieBookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieBookingSystemApplication.class, args);
    }
}
```

#### **3.5. Database Migration (Flyway/Liquibase or Manual)**

Nếu dùng Flyway, tạo migration:

```sql
-- File: src/main/resources/db/migration/V2__add_booking_expires_at.sql

ALTER TABLE bookings 
ADD COLUMN expires_at TIMESTAMP NULL;

CREATE INDEX idx_booking_expires_at ON bookings(expires_at);

-- Update existing PENDING_PAYMENT bookings
UPDATE bookings 
SET expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE)
WHERE status = 'PENDING_PAYMENT' AND expires_at IS NULL;
```

Hoặc chạy manual trong MySQL:

```sql
ALTER TABLE bookings ADD COLUMN expires_at TIMESTAMP NULL;
CREATE INDEX idx_booking_expires_at ON bookings(expires_at);
UPDATE bookings SET expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE) 
WHERE status = 'PENDING_PAYMENT' AND expires_at IS NULL;
```

### **Testing:**

```bash
# Test 1: Create booking và verify expires_at được set
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer {token}" \
  -d '{"showtimeId": 1, "seatIds": [1,2,3]}'
  
# Check DB:
SELECT id, booking_date, expires_at, status FROM bookings WHERE id = LAST_INSERT_ID();
# Expected: expires_at = booking_date + 15 minutes

# Test 2: Manually trigger cron (for testing)
# Add REST endpoint temporarily:
@GetMapping("/test/expire-bookings")
public ResponseEntity<?> testExpiration() {
    bookingExpirationService.expireBookings();
    return ResponseEntity.ok("OK");
}

# Test 3: Wait for auto-run (every 5 minutes)
# Check logs:
grep "EXPIRATION" logs/application.log
```

### **Commit Message:**
```
feat(booking): add automatic expiration for unpaid bookings

- Add expiresAt field to Booking entity (15min timeout)
- Create BookingExpirationService with @Scheduled cron job
- Auto-expire PENDING_PAYMENT bookings every 5 minutes
- Cancel associated payment transactions
- Release seat holds to make them available
- Add database migration for expires_at column

Fixes: #2
```

---

## 🚀 STEP 4: Add VNPay IPN Webhook (3 hours)

### **Vấn đề:**
Chỉ có Return URL → không reliable khi user đóng browser.

### **Fix Location:**
`PaymentController.java` - Add IPN endpoint

### **Code Changes:**

#### **4.1. Add IPN endpoint in PaymentController**

```java
// File: src/main/java/com/trainning/movie_booking_system/controller/PaymentController.java

/**
 * VNPay IPN (Instant Payment Notification) webhook
 * VNPay sẽ gọi endpoint này TRỰC TIẾP (server-to-server) sau khi payment
 * AUTO-RETRY lên đến 10 lần nếu fail
 * 
 * PUBLIC - No authentication (verified by VNPay signature)
 * POST /api/v1/payments/vnpay/ipn
 * 
 * ⚠️ CRITICAL: MUST return status code 200 với format VNPay yêu cầu
 * Nếu không VNPay sẽ retry liên tục!
 */
@PostMapping("/vnpay/ipn")
public ResponseEntity<?> handleVNPayIPN(HttpServletRequest request) {
    log.info("[PAYMENT-IPN] Received VNPay IPN from IP: {}", request.getRemoteAddr());
    
    String vnpTxnRef = request.getParameter("vnp_TxnRef");
    
    try {
        // Reuse same payment processing logic (idempotent)
        PaymentResponse response = paymentService.handleVNPayReturn(request);
        
        // VNPay expects specific response format
        Map<String, String> vnpResponse = new HashMap<>();
        
        // Success cases: SUCCESS, REFUND_PENDING (already processed)
        if ("SUCCESS".equals(response.getStatus()) || 
            response.getStatus().startsWith("REFUND") ||
            response.getMessage().contains("already processed")) {
            
            vnpResponse.put("RspCode", "00");
            vnpResponse.put("Message", "Confirm Success");
            
            log.info("[PAYMENT-IPN] ✅ Responded to VNPay for txn {}: SUCCESS", vnpTxnRef);
            
        } else {
            // Failed/Cancelled
            vnpResponse.put("RspCode", "99");
            vnpResponse.put("Message", "Unknown error");
            
            log.warn("[PAYMENT-IPN] ⚠️ Responded to VNPay for txn {}: FAILED", vnpTxnRef);
        }
        
        return ResponseEntity.ok(vnpResponse);
        
    } catch (Exception e) {
        log.error("[PAYMENT-IPN] ❌ Error processing IPN for txn {}", vnpTxnRef, e);
        
        // Return error để VNPay retry
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("RspCode", "99");
        errorResponse.put("Message", e.getMessage());
        
        // Return 500 để VNPay biết cần retry
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

#### **4.2. Update VnPayProperties - Add IPN URL**

```java
// File: src/main/java/com/trainning/movie_booking_system/config/VnPayProperties.java

@Data
@Component
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnPayProperties {
    private String tmnCode;
    private String hashSecret;
    private String payUrl;
    private String returnUrl;
    private String ipnUrl; // ✅ Add this
}
```

#### **4.3. Update application.yml**

```yaml
# File: src/main/resources/application.yml

payment:
  vnpay:
    tmnCode: ${VNPAY_TMN_CODE:YOUR_TMN_CODE}
    hashSecret: ${VNPAY_HASH_SECRET:YOUR_HASH_SECRET}
    payUrl: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    returnUrl: http://localhost:8080/api/v1/payments/vnpay/callback
    ipnUrl: http://localhost:8080/api/v1/payments/vnpay/ipn  # ✅ Add this
    
    # ⚠️ PRODUCTION: Phải dùng public domain
    # returnUrl: https://yourdomain.com/api/v1/payments/vnpay/callback
    # ipnUrl: https://yourdomain.com/api/v1/payments/vnpay/ipn
```

#### **4.4. Update VnPayServiceImpl - Include IPN URL**

Nếu VnPayService có method `createPaymentUrl`, đảm bảo nó include IPN URL:

```java
// Thường VNPay không cần truyền IPN URL trong request
// Bạn config IPN URL trực tiếp trên VNPay merchant portal
// Nhưng một số gateway khác (MoMo, ZaloPay) yêu cầu pass qua API
```

### **Testing:**

#### **Local Testing với ngrok:**

```bash
# Terminal 1: Start app
mvn spring-boot:run

# Terminal 2: Expose localhost
ngrok http 8080

# Output:
# Forwarding: https://abc123.ngrok.io -> http://localhost:8080

# Cập nhật VNPay merchant config:
# IPN URL = https://abc123.ngrok.io/api/v1/payments/vnpay/ipn
```

#### **Manual Test:**

```bash
# Simulate VNPay IPN call
curl -X POST "http://localhost:8080/api/v1/payments/vnpay/ipn" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "vnp_TxnRef=TXN_123&vnp_TransactionStatus=00&vnp_SecureHash=..."
  
# Expected Response:
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

#### **Test Idempotency:**

```bash
# Call IPN 3 lần liên tiếp (giống VNPay retry)
for i in {1..3}; do
  curl -X POST "http://localhost:8080/api/v1/payments/vnpay/ipn" \
    -d "vnp_TxnRef=TXN_456&vnp_TransactionStatus=00&vnp_SecureHash=..."
done

# Expected: 
# - Lần 1: Process payment → SUCCESS
# - Lần 2,3: Idempotent response → "already processed"
# - Booking chỉ confirm 1 lần
```

### **Commit Message:**
```
feat(payment): add VNPay IPN webhook for reliable payment notifications

- Add POST /vnpay/ipn endpoint for server-to-server callbacks
- Reuse handleVNPayReturn logic (already idempotent)
- Return VNPay-compliant response format (RspCode, Message)
- Add ipnUrl config in application.yml
- Handle errors with 500 status to trigger VNPay retry

Fixes: #4
```

---

## 🚀 STEP 5: Add Payment Webhook Logging (2 hours)

(TIẾP TỤC trong phần tiếp theo...)

---

## 📝 CHECKLIST - Sau khi hoàn thành mỗi step:

- [ ] Code compile không lỗi
- [ ] Test cases pass
- [ ] Git commit với message chuẩn
- [ ] Update CHANGELOG.md (optional)
- [ ] Manual test trên Postman
- [ ] Review code lần cuối

---

**BẠN MUỐN:**
1. ✅ Tôi hướng dẫn tiếp **Step 5, 6** (payment logging + seat release)?
2. ✅ Bạn làm Step 1-4 trước rồi báo tôi kết quả?
3. ✅ Tôi tạo branch và PR template ngay?

**Chọn đi!**
