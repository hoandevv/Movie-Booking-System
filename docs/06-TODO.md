# 🎯 TODO - Implementation Tasks

> **Last Updated:** November 11, 2025  
> **Purpose:** Track pending improvements and critical fixes

---

## 🚨 CRITICAL (Must Fix Before Production)

### 1. Payment Gateway Integration

**Status:** ⚠️ MOCK Implementation  
**Priority:** 🔴 CRITICAL  
**Effort:** 2-3 days

**Current State:**
- Payment URL is hardcoded mock
- No signature verification from gateway
- No actual payment processing

**Implementation Plan:**

```java
// TODO: VNPayServiceImpl.java
public String createPaymentUrl(PaymentRequest request) {
    // Build VNPay request
    Map<String, String> vnpParams = new HashMap<>();
    vnpParams.put("vnp_TmnCode", tmnCode);
    vnpParams.put("vnp_Amount", String.valueOf(request.getAmount() * 100));
    vnpParams.put("vnp_OrderInfo", request.getOrderInfo());
    // ... more params
    
    // Calculate signature (HMAC SHA512)
    String signData = buildSignData(vnpParams);
    String signature = HmacSHA512(hashSecret, signData);
    vnpParams.put("vnp_SecureHash", signature);
    
    // Build URL
    return VNPAY_URL + "?" + buildQueryString(vnpParams);
}

// TODO: PaymentController.java
@PostMapping("/vnpay/callback")
public ResponseEntity<?> vnpayCallback(@RequestParam Map<String, String> params) {
    // 1. VERIFY SIGNATURE (CRITICAL!)
    String receivedHash = params.get("vnp_SecureHash");
    params.remove("vnp_SecureHash");
    String calculatedHash = calculateHash(params);
    
    if (!receivedHash.equals(calculatedHash)) {
        throw new SecurityException("Invalid payment signature");
    }
    
    // 2. Process payment...
}
```

**Files to Create/Modify:**
- `VnPayServiceImpl.java` - Real implementation
- `PaymentController.java` - Add signature verification
- `application.yml` - Add VNPay credentials

**Related:** [Payment Flow Documentation](docs/05-PAYMENT-FLOW.md)

---

### 2. Payment IPN Webhook

**Status:** ❌ NOT IMPLEMENTED  
**Priority:** 🔴 CRITICAL  
**Effort:** 4 hours

**Why Critical:**
- Return URL callback depends on user browser (can be closed)
- IPN is server-to-server notification (reliable)
- VNPay auto-retries IPN up to 10 times

**Implementation:**

```java
// TODO: PaymentController.java
@PostMapping("/vnpay/ipn")
public ResponseEntity<Map<String, String>> vnpayIPN(
    @RequestParam Map<String, String> params,
    HttpServletRequest request
) {
    // Log webhook
    log.info("VNPay IPN received: {}", params);
    
    // Verify signature
    if (!verifySignature(params)) {
        return ResponseEntity.ok(Map.of(
            "RspCode", "97",
            "Message", "Invalid signature"
        ));
    }
    
    // Reuse payment processing logic
    PaymentResponse response = paymentService.handleVNPayReturn(request);
    
    // Return VNPay format
    return ResponseEntity.ok(Map.of(
        "RspCode", "00",
        "Message", "Confirm Success"
    ));
}
```

**VNPay IPN Response Codes:**
- `00` - Success (VNPay stops retry)
- `99` - Error (VNPay retries)

---

### 3. Payment Idempotency

**Status:** ⚠️ PARTIALLY IMPLEMENTED  
**Priority:** 🔴 CRITICAL  
**Effort:** 3 hours

**Issue:**
Multiple callbacks for same payment can cause duplicate processing.

**Current Implementation (Missing):**
```java
// BEFORE (Vulnerable to race condition):
public PaymentResponse handleCallback(String txnRef) {
    PaymentTransaction txn = findByTxnRef(txnRef);
    // Process payment... (can run multiple times!)
}
```

**Required Implementation:**
```java
// AFTER (With idempotency):
@Transactional(isolation = Isolation.SERIALIZABLE)
public PaymentResponse handleCallback(String txnRef) {
    // 1. Acquire Redis lock
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(
        "payment:lock:" + txnRef, "locked", 30, TimeUnit.SECONDS
    );
    
    if (!locked) {
        throw new ConflictException("Payment is being processed");
    }
    
    try {
        // 2. Load with pessimistic lock
        PaymentTransaction txn = repository.findByTxnRefForUpdate(txnRef);
        
        // 3. IDEMPOTENCY CHECK
        if (txn.getStatus() != PaymentStatus.PENDING) {
            log.warn("Duplicate callback for txn: {}", txnRef);
            return buildCachedResponse(txn);
        }
        
        // 4. Process (only if PENDING)
        return processPayment(txn);
        
    } finally {
        // 5. Release lock
        redisTemplate.delete("payment:lock:" + txnRef);
    }
}
```

**Files to Modify:**
- `PaymentServiceImpl.java`
- `PaymentTransactionRepository.java` (add `@Lock(PESSIMISTIC_WRITE)`)

---

### 4. Booking Expiration

**Status:** ⚠️ PARTIALLY IMPLEMENTED  
**Priority:** 🔴 CRITICAL  
**Effort:** 3 hours

**Issue:**
Bookings don't have `expiresAt` field, can't auto-expire.

**Database Migration:**

```sql
-- Add expires_at column
ALTER TABLE bookings
ADD COLUMN expires_at TIMESTAMP NULL AFTER booking_date;

-- Create index for cron query
CREATE INDEX idx_booking_expires_at ON bookings(expires_at);
```

**Entity Update:**

```java
@Entity
public class Booking {
    // ... existing fields
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    public void setExpiresAt() {
        if (this.status == BookingStatus.PENDING_PAYMENT) {
            this.expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }
}
```

**Cron Job:**

```java
@Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
public void expireBookings() {
    List<Booking> expiredBookings = bookingRepository
        .findByStatusAndExpiresAtBefore(
            BookingStatus.PENDING_PAYMENT,
            LocalDateTime.now()
        );
    
    for (Booking booking : expiredBookings) {
        booking.setStatus(BookingStatus.EXPIRED);
        
        // Cancel payment transactions
        paymentService.cancelPayment(booking.getId());
        
        // Release seat holds (if any)
        seatDomainService.releaseHolds(booking.getSeatIds());
    }
    
    bookingRepository.saveAll(expiredBookings);
    log.info("Expired {} bookings", expiredBookings.size());
}
```

---

## 🔶 MAJOR (High Priority)

### 5. Showtime Validation

**Status:** ❌ NOT IMPLEMENTED  
**Priority:** 🟠 MAJOR  
**Effort:** 30 minutes

**Issue:**
User can book for past showtimes or showtimes that already started.

**Implementation:**

```java
// BookingServiceImpl.java
private void validateShowtime(Showtime showtime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime showtimeStart = LocalDateTime.of(
        showtime.getShowDate(),
        showtime.getStartTime()
    );
    
    // Can't book for past showtime
    if (showtimeStart.isBefore(now)) {
        throw new BadRequestException("Cannot book for past showtime");
    }
    
    // Booking closes 15 minutes before showtime
    LocalDateTime cutoff = showtimeStart.minusMinutes(15);
    if (now.isAfter(cutoff)) {
        throw new BadRequestException(
            "Booking closes 15 minutes before showtime"
        );
    }
}
```

---

### 6. Email Templates

**Status:** ⚠️ BASIC IMPLEMENTATION  
**Priority:** 🟠 MAJOR  
**Effort:** 1 day

**Current:** Plain text emails  
**Required:** HTML templates with branding

**Implementation:**

```java
// EmailService.java
public void sendBookingConfirmation(Booking booking) {
    Context context = new Context();
    context.setVariable("booking", booking);
    context.setVariable("qrCode", generateQRCode(booking.getId()));
    
    String html = templateEngine.process("booking-confirmation", context);
    
    sendEmail(
        booking.getUser().getEmail(),
        "Booking Confirmation - " + booking.getShowtime().getMovie().getTitle(),
        html
    );
}
```

**Templates to Create:**
- `templates/booking-confirmation.html`
- `templates/otp-verification.html`
- `templates/password-reset.html`

---

### 7. Pagination & Filtering

**Status:** ⚠️ PARTIALLY IMPLEMENTED  
**Priority:** 🟠 MAJOR  
**Effort:** 2 hours

**Missing:**
- Booking pagination (throws `UnsupportedOperationException`)
- Filter by status, date range, user

**Implementation:**

```java
// BookingController.java
@GetMapping
public ResponseEntity<Page<BookingDTO>> getBookings(
    @RequestParam(required = false) BookingStatus status,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
    Pageable pageable
) {
    Page<Booking> bookings = bookingService.findBookings(
        status, startDate, endDate, pageable
    );
    return ResponseEntity.ok(bookings.map(mapper::toDTO));
}

// BookingService.java (use Specification API)
public Page<Booking> findBookings(
    BookingStatus status,
    LocalDate startDate,
    LocalDate endDate,
    Pageable pageable
) {
    Specification<Booking> spec = Specification.where(null);
    
    if (status != null) {
        spec = spec.and((root, query, cb) -> 
            cb.equal(root.get("status"), status)
        );
    }
    
    if (startDate != null) {
        spec = spec.and((root, query, cb) -> 
            cb.greaterThanOrEqualTo(root.get("bookingDate"), startDate.atStartOfDay())
        );
    }
    
    // ... more filters
    
    return repository.findAll(spec, pageable);
}
```

---

## 🔷 MINOR (Nice to Have)

### 8. Logging & Monitoring

**Priority:** 🟡 MINOR  
**Effort:** 1 day

**Improvements:**

```java
// Add structured logging
@Slf4j
public class BookingService {
    public Booking create(BookingRequest request) {
        MDC.put("bookingId", bookingId);
        MDC.put("userId", userId);
        
        log.info("Creating booking: showtimeId={}, seatCount={}", 
            request.getShowtimeId(), request.getSeatIds().size());
        
        try {
            // ... business logic
            log.info("Booking created successfully");
        } catch (Exception e) {
            log.error("Failed to create booking", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

**Add Actuator endpoints:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

### 9. API Rate Limiting

**Priority:** 🟡 MINOR  
**Effort:** 4 hours

**Implementation with Bucket4j:**

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String userId = SecurityUtils.getCurrentUserId();
        
        Bucket bucket = cache.computeIfAbsent(userId, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Rate limit exceeded");
        }
    }
    
    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(100)
            .refillIntervally(100, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
```

---

### 10. Internationalization (i18n)

**Priority:** 🟡 MINOR  
**Effort:** 2 hours

**Externalize error messages:**

```properties
# messages.properties
error.booking.seat_not_available=Seat {0} is not available
error.booking.showtime_started=Cannot book for started showtime
error.auth.invalid_credentials=Invalid username or password

# messages_vi.properties
error.booking.seat_not_available=Ghế {0} không còn trống
error.booking.showtime_started=Không thể đặt vé cho suất chiếu đã bắt đầu
error.auth.invalid_credentials=Tên đăng nhập hoặc mật khẩu không đúng
```

---

## 📊 PROGRESS TRACKING

| Task | Priority | Status | Assignee | Due Date |
|------|----------|--------|----------|----------|
| Payment Gateway Integration | 🔴 CRITICAL | ⚠️ TODO | - | - |
| Payment IPN Webhook | 🔴 CRITICAL | ❌ TODO | - | - |
| Payment Idempotency | 🔴 CRITICAL | ⚠️ TODO | - | - |
| Booking Expiration | 🔴 CRITICAL | ⚠️ TODO | - | - |
| Showtime Validation | 🟠 MAJOR | ❌ TODO | - | - |
| Email Templates | 🟠 MAJOR | ⚠️ TODO | - | - |
| Pagination & Filtering | 🟠 MAJOR | ⚠️ TODO | - | - |
| Logging & Monitoring | 🟡 MINOR | ❌ TODO | - | - |
| API Rate Limiting | 🟡 MINOR | ❌ TODO | - | - |
| Internationalization | 🟡 MINOR | ❌ TODO | - | - |

---

## 🎯 RECOMMENDED IMPLEMENTATION ORDER

### Week 1: Critical Fixes
1. ✅ Showtime validation (30 min)
2. ✅ Payment idempotency (3 hrs)
3. ✅ Booking expiration (3 hrs)
4. ✅ Payment IPN webhook (4 hrs)

### Week 2: Payment Integration
5. ✅ VNPay integration (2 days)
6. ✅ Payment testing (1 day)

### Week 3: Enhancements
7. ✅ Email templates (1 day)
8. ✅ Pagination & filtering (2 hrs)
9. ✅ Logging improvements (1 day)

### Week 4: Polish
10. ✅ API rate limiting (4 hrs)
11. ✅ i18n (2 hrs)
12. ✅ Final testing & deployment

---

## 📚 RELATED DOCS

- [Booking Flow](04-BOOKING-FLOW.md) - For booking-related tasks
- [Payment Flow](05-PAYMENT-FLOW.md) - For payment tasks
- [Testing Guide](07-TESTING-GUIDE.md) - Test each implementation

---

**💡 Start with CRITICAL tasks first!**
