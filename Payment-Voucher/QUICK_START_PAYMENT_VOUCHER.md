# 🚀 Quick Start Guide - Payment & Voucher Implementation

> Hướng dẫn nhanh để bắt đầu implement từng task

---

## 📋 Overview

Branch: `feature/payment-voucher-integration`

**Tổng quan:**
- 4 database tables mới
- 14 API endpoints (7 payment + 4 voucher + 3 admin)
- VNPay + Stripe integration
- 8-step voucher validation
- Signature verification + Idempotency

**Timeline:** 7 days (6 phases)

---

## 📁 File Structure

```
src/main/java/com/trainning/movie_booking_system/
├── config/
│   ├── VNPayConfig.java          ← DAY 3
│   └── StripeConfig.java         ← DAY 5
├── controller/
│   ├── PaymentController.java    ← DAY 6
│   └── VoucherController.java    ← DAY 2
├── dto/
│   ├── request/
│   │   ├── CreatePaymentRequest.java
│   │   └── CreateVoucherRequest.java
│   └── response/
│       ├── PaymentResponse.java
│       ├── VoucherResponse.java
│       ├── VoucherValidationResult.java
│       └── VoucherUsageResponse.java
├── entity/
│   ├── PaymentTransaction.java   ← DAY 1
│   ├── Voucher.java              ← DAY 1
│   ├── VoucherUsage.java         ← DAY 1
│   └── PaymentWebhookLog.java    ← DAY 1
├── repository/
│   ├── PaymentTransactionRepository.java
│   ├── VoucherRepository.java
│   ├── VoucherUsageRepository.java
│   └── PaymentWebhookLogRepository.java
├── service/
│   ├── IVoucherService.java
│   ├── IPaymentService.java
│   ├── impl/
│   │   ├── VoucherServiceImpl.java     ← DAY 2
│   │   └── PaymentServiceImpl.java     ← DAY 6 (REFACTOR)
│   └── payment/
│       ├── VNPayService.java           ← DAY 3-4
│       └── StripeService.java          ← DAY 5
└── utils/enums/
    ├── PaymentGateway.java
    ├── PaymentStatus.java
    ├── VoucherDiscountType.java
    └── VoucherStatus.java

src/main/resources/
├── db/migration/
│   └── V5__create_payment_voucher_tables.sql  ← DAY 1
└── application.yml                             ← DAY 3-5 (add configs)

src/test/java/
├── service/impl/
│   └── VoucherServiceTest.java                ← DAY 7
└── integration/
    └── PaymentIntegrationTest.java            ← DAY 7
```

---

## 📅 DAY 1: Database & Entities (4-6 hours)

### Task 1.1: Add Dependencies to pom.xml
```xml
<!-- Open: pom.xml -->
<!-- Add before </dependencies> -->

<!-- Stripe SDK -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.16.0</version>
</dependency>

<!-- HMAC for signature -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.16.0</version>
</dependency>

<!-- JSON parsing -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

**Run:**
```bash
mvn clean install
```

---

### Task 1.2: Create Database Migration

**File:** `src/main/resources/db/migration/V5__create_payment_voucher_tables.sql`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION.md` → Step 2

**Verify:**
```bash
mvn flyway:migrate
# Check MySQL:
mysql -u root -p
USE movie_booking_system;
SHOW TABLES;  # Should see: payment_transactions, vouchers, voucher_usages, payment_webhook_logs
```

---

### Task 1.3: Create Enums

Create 4 files in `utils/enums/`:
1. `PaymentGateway.java`
2. `PaymentStatus.java`
3. `VoucherDiscountType.java`
4. `VoucherStatus.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION.md` → Step 3

---

### Task 1.4: Create Entities

Create 4 files in `entity/`:
1. `PaymentTransaction.java`
2. `Voucher.java`
3. `VoucherUsage.java`
4. `PaymentWebhookLog.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION.md` → Step 4

---

### Task 1.5: Create Repositories

Create 4 files in `repository/`:
1. `PaymentTransactionRepository.java`
2. `VoucherRepository.java`
3. `VoucherUsageRepository.java`
4. `PaymentWebhookLogRepository.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION.md` → Step 5

---

### Day 1 Checklist
- [ ] Dependencies added to pom.xml
- [ ] Database migration V5 created & executed
- [ ] 4 tables exist in database
- [ ] 4 enums created
- [ ] 4 entities created
- [ ] 4 repositories created
- [ ] Application compiles without errors

**Test:**
```bash
mvn clean compile
# Should show: BUILD SUCCESS
```

---

## 📅 DAY 2: Voucher Service (6-8 hours)

### Task 2.1: Create DTOs

Create in `dto/response/`:
1. `VoucherValidationResult.java`
2. `VoucherResponse.java`
3. `VoucherUsageResponse.java`

Create in `dto/request/`:
1. `CreateVoucherRequest.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 6 & Part 3 → Step 10

---

### Task 2.2: Create VoucherMapper

**File:** `mapper/VoucherMapper.java`

```java
@Mapper(componentModel = "spring")
public interface VoucherMapper {
    VoucherResponse toResponse(Voucher voucher);
    VoucherUsageResponse toUsageResponse(VoucherUsage usage);
    Voucher toEntity(CreateVoucherRequest request);
    void updateEntity(CreateVoucherRequest request, @MappingTarget Voucher voucher);
}
```

---

### Task 2.3: Create IVoucherService Interface

**File:** `service/IVoucherService.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 6

---

### Task 2.4: Implement VoucherServiceImpl

**File:** `service/impl/VoucherServiceImpl.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 6

⚠️ **Quan trọng:**
- Đọc kỹ 8-step validation logic
- Test từng validation case
- Đảm bảo discount calculation đúng

---

### Task 2.5: Create VoucherController

**File:** `controller/VoucherController.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 10

---

### Day 2 Checklist
- [ ] VoucherValidationResult.java created
- [ ] VoucherMapper created
- [ ] IVoucherService created
- [ ] VoucherServiceImpl created (500+ LOC)
- [ ] VoucherController created
- [ ] Application compiles
- [ ] Can test voucher validation via Postman

**Test API:**
```bash
# Create voucher (admin only)
POST http://localhost:8080/api/vouchers/admin
Authorization: Bearer <admin_token>
{
  "code": "TEST20",
  "name": "Test 20% Off",
  "discountType": "PERCENTAGE",
  "discountValue": 20,
  "minOrderAmount": 100000,
  "totalUsageLimit": 100,
  "usagePerUser": 1,
  "validFrom": "2024-01-01T00:00:00",
  "validUntil": "2024-12-31T23:59:59",
  "status": "ACTIVE",
  "isPublic": true
}

# Validate voucher
POST http://localhost:8080/api/vouchers/validate?voucherCode=TEST20&bookingId=1&bookingAmount=300000
```

---

## 📅 DAY 3-4: VNPay Integration (8-12 hours)

### Task 3.1: Create VNPayConfig

**File:** `config/VNPayConfig.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 7

---

### Task 3.2: Update application.yml

**File:** `src/main/resources/application.yml`

```yaml
vnpay:
  tmn-code: ${VNPAY_TMN_CODE:YOUR_TMN_CODE}
  hash-secret: ${VNPAY_HASH_SECRET:YOUR_SECRET_KEY}
  url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  return-url: ${FRONTEND_URL:http://localhost:3000}/payment/vnpay-return
  ipn-url: ${BACKEND_URL:http://localhost:8080}/api/payments/vnpay/ipn
```

---

### Task 3.3: Create VNPayService

**File:** `service/payment/VNPayService.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 7

⚠️ **Critical:**
- Hiểu signature generation algorithm (HMAC-SHA512)
- Test signature verification carefully
- Log all signature mismatches

---

### Task 3.4: Test VNPay Signature

**Create test:**
```java
@Test
void testVNPaySignatureGeneration() {
    Map<String, String> params = new TreeMap<>();
    params.put("vnp_Amount", "10000000");
    params.put("vnp_Command", "pay");
    params.put("vnp_TmnCode", "TEST");
    
    String signature = vnPayService.generateSignature(params);
    assertNotNull(signature);
    assertEquals(128, signature.length()); // HMAC-SHA512 = 128 hex chars
}
```

---

### Day 3-4 Checklist
- [ ] VNPayConfig created
- [ ] application.yml updated
- [ ] VNPayService created
- [ ] Signature generation tested
- [ ] Signature verification tested
- [ ] Can generate VNPay payment URL
- [ ] URL contains valid signature

**Test:**
```java
String paymentUrl = vnPayService.createPaymentUrl(
    "ORDER123",
    300000L,
    "Test payment",
    "127.0.0.1"
);
System.out.println(paymentUrl);
// Should contain: vnp_SecureHash=<128_hex_chars>
```

---

## 📅 DAY 5: Stripe Integration (6-8 hours)

### Task 5.1: Create StripeConfig

**File:** `config/StripeConfig.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 8

---

### Task 5.2: Update application.yml

```yaml
stripe:
  api-key: ${STRIPE_API_KEY:sk_test_YOUR_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:whsec_YOUR_SECRET}
  success-url: ${FRONTEND_URL:http://localhost:3000}/payment/success?session_id={CHECKOUT_SESSION_ID}
  cancel-url: ${FRONTEND_URL:http://localhost:3000}/payment/cancel
```

---

### Task 5.3: Create StripeService

**File:** `service/payment/StripeService.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 8

---

### Task 5.4: Test Stripe Checkout

```java
@Test
void testStripeCheckoutSession() throws StripeException {
    Session session = stripeService.createCheckoutSession(
        "ORDER123",
        BigDecimal.valueOf(300000),
        "Test payment",
        "test@example.com"
    );
    
    assertNotNull(session.getId());
    assertNotNull(session.getUrl());
    assertEquals("ORDER123", session.getClientReferenceId());
}
```

---

### Day 5 Checklist
- [ ] StripeConfig created
- [ ] application.yml updated
- [ ] StripeService created
- [ ] Can create checkout session
- [ ] Webhook signature verification works
- [ ] Test with Stripe test card: 4242 4242 4242 4242

---

## 📅 DAY 6: PaymentService Refactoring (8-10 hours)

### Task 6.1: Create DTOs

1. `CreatePaymentRequest.java`
2. `PaymentResponse.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 10

---

### Task 6.2: Refactor PaymentServiceImpl

**File:** `service/impl/PaymentServiceImpl.java`

**IMPORTANT:** Backup existing file first!

```bash
cp service/impl/PaymentServiceImpl.java service/impl/PaymentServiceImpl.java.backup
```

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART2.md` → Step 9

⚠️ **Key Features:**
- Voucher application
- Idempotency check (Redis)
- Signature verification
- Webhook logging
- Email confirmation

---

### Task 6.3: Create PaymentController

**File:** `controller/PaymentController.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 10

---

### Task 6.4: Add Missing Methods to IPaymentService

```java
public interface IPaymentService {
    PaymentResponse createPaymentUrl(CreatePaymentRequest request, String ipAddress, Long userId);
    void handleVNPayCallback(Map<String, String> params);
    void handleStripeWebhook(String payload, String sigHeader);
    PaymentResponse verifyPaymentStatus(String transactionId, Long userId);
    void cancelPayment(String transactionId, Long userId);
}
```

---

### Day 6 Checklist
- [ ] PaymentServiceImpl refactored
- [ ] PaymentController created
- [ ] All 7 payment endpoints working
- [ ] VNPay IPN callback tested
- [ ] Stripe webhook tested
- [ ] Idempotency check works
- [ ] Email sent on successful payment

**Test End-to-End Flow:**
```bash
# 1. Create booking
POST /api/bookings

# 2. Validate voucher (optional)
POST /api/vouchers/validate?voucherCode=TEST20&bookingId=1&bookingAmount=300000

# 3. Create payment
POST /api/payments/create
{
  "bookingId": 1,
  "gatewayType": "VNPAY",
  "voucherCode": "TEST20"
}

# 4. Redirect to payment URL
# 5. Complete payment
# 6. IPN callback triggers
# 7. Check booking status → CONFIRMED
# 8. Check email → Confirmation sent
```

---

## 📅 DAY 7: Testing & Documentation (6-8 hours)

### Task 7.1: Unit Tests

**File:** `test/service/impl/VoucherServiceTest.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 11

**Run:**
```bash
mvn test -Dtest=VoucherServiceTest
```

---

### Task 7.2: Integration Tests

**File:** `test/integration/PaymentIntegrationTest.java`

**Copy từ:** `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 11

**Run:**
```bash
mvn verify
```

---

### Task 7.3: Manual Testing

Follow test scenarios in `PAYMENT_VOUCHER_IMPLEMENTATION_PART3.md` → Step 12

**VNPay Sandbox:**
- Card: 9704198526191432198
- OTP: 123456

**Stripe Test:**
- Card: 4242 4242 4242 4242

---

### Task 7.4: Update Documentation

1. Update `SEQUENCE_DIAGRAMS.md` with payment flows
2. Update Postman collection
3. Update `README.md` with new endpoints

---

### Day 7 Checklist
- [ ] Unit tests pass (mvn test)
- [ ] Integration tests pass (mvn verify)
- [ ] VNPay sandbox tested
- [ ] Stripe sandbox tested
- [ ] All 39 acceptance criteria met
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Ready for production

---

## 🔍 Review Checklist (Ask me to review!)

### Code Quality
- [ ] No hardcoded secrets (use environment variables)
- [ ] Proper exception handling
- [ ] Logging at appropriate levels
- [ ] Transaction boundaries correct (@Transactional)
- [ ] No N+1 query problems

### Security
- [ ] Signature verification works
- [ ] Idempotency prevents duplicate processing
- [ ] Amount validation prevents tampering
- [ ] Sensitive data not logged
- [ ] HTTPS enforced in production

### Performance
- [ ] Database queries optimized
- [ ] Indexes on transaction_id, booking_id
- [ ] Redis caching for idempotency
- [ ] Async email sending

### Testing
- [ ] 80%+ code coverage
- [ ] All edge cases tested
- [ ] Integration tests pass
- [ ] Manual testing completed

---

## 🚨 Common Issues & Solutions

### Issue 1: Signature Mismatch (VNPay)
**Problem:** `vnp_SecureHash` không khớp

**Solution:**
1. Log `signData` before hashing
2. Verify params are sorted alphabetically
3. Check URL encoding
4. Verify secret key matches VNPay portal

---

### Issue 2: Stripe Webhook 401
**Problem:** Webhook signature invalid

**Solution:**
1. Use Stripe CLI for local testing:
   ```bash
   stripe listen --forward-to localhost:8080/api/payments/stripe/webhook
   ```
2. Copy webhook signing secret from CLI output
3. Update `application.yml`

---

### Issue 3: Duplicate IPN Callbacks
**Problem:** Payment processed multiple times

**Solution:**
1. Check Redis idempotency cache
2. Verify key: `payment:idempotency:{transactionId}`
3. TTL should be 24 hours
4. Check logs for "Duplicate callback" warning

---

### Issue 4: Voucher Not Applied
**Problem:** Discount = 0

**Solution:**
1. Check validation result: `POST /api/vouchers/validate`
2. Verify 8 validation steps pass
3. Check min order amount
4. Verify user hasn't exceeded usage limit

---

## 📞 Need Help?

**Khi nào cần tôi review:**
1. ✅ Sau mỗi DAY hoàn thành
2. ✅ Khi gặp lỗi không debug được
3. ✅ Trước khi merge vào develop
4. ✅ Trước khi deploy production

**Tag tôi với:**
- Code snippet (nếu có lỗi)
- Error message
- Test case failing
- Performance concerns

---

## 🎯 Success Criteria

**Khi nào coi như hoàn thành:**

✅ All 39 acceptance criteria met
✅ Tests pass (unit + integration)
✅ VNPay sandbox works
✅ Stripe sandbox works
✅ Code reviewed & approved
✅ Documentation complete
✅ Ready for production deployment

**Good luck! 🚀**

Start với Day 1 và tag tôi khi xong để review nhé!
