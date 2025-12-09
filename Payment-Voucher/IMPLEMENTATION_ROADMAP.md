# 🗺️ Payment & Voucher Implementation Roadmap

> Track your progress throughout the 7-day implementation

**Branch:** `feature/payment-voucher-integration`  
**Start Date:** _____________  
**Target Completion:** _____________  

---

## 📊 Overall Progress

```
[Day 1] ░░░░░░░░░░ 0/10 tasks
[Day 2] ░░░░░░░░░░ 0/10 tasks
[Day 3-4] ░░░░░░░░░░ 0/10 tasks
[Day 5] ░░░░░░░░░░ 0/8 tasks
[Day 6] ░░░░░░░░░░ 0/12 tasks
[Day 7] ░░░░░░░░░░ 0/10 tasks

Total: 0/60 tasks (0%)
```

---

## 📅 Day 1: Database & Entities

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### Setup (2 tasks)
- [ ] **1.1** Add dependencies to `pom.xml` (Stripe, Commons Codec, Gson)
- [ ] **1.2** Run `mvn clean install` successfully

#### Database (2 tasks)
- [ ] **1.3** Create migration `V5__create_payment_voucher_tables.sql`
- [ ] **1.4** Run `mvn flyway:migrate` and verify 4 new tables

#### Enums (1 task)
- [ ] **1.5** Create 4 enums:
  - [ ] PaymentGateway.java
  - [ ] PaymentStatus.java
  - [ ] VoucherDiscountType.java
  - [ ] VoucherStatus.java

#### Entities (1 task)
- [ ] **1.6** Create 4 entities:
  - [ ] PaymentTransaction.java (~120 LOC)
  - [ ] Voucher.java (~150 LOC)
  - [ ] VoucherUsage.java (~60 LOC)
  - [ ] PaymentWebhookLog.java (~80 LOC)

#### Repositories (1 task)
- [ ] **1.7** Create 4 repositories:
  - [ ] PaymentTransactionRepository.java
  - [ ] VoucherRepository.java
  - [ ] VoucherUsageRepository.java
  - [ ] PaymentWebhookLogRepository.java

#### Verification (3 tasks)
- [ ] **1.8** Run `mvn clean compile` → BUILD SUCCESS
- [ ] **1.9** Check database: `SHOW TABLES;` → 4 new tables visible
- [ ] **1.10** Commit: `git commit -m "Day 1: Database schema & entities"`

### Day 1 Checklist
- [ ] All dependencies installed
- [ ] Database migration successful
- [ ] 4 enums created
- [ ] 4 entities created with proper annotations
- [ ] 4 repositories with custom queries
- [ ] Application compiles without errors
- [ ] Code committed to branch

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## 📅 Day 2: Voucher Service

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### DTOs (2 tasks)
- [ ] **2.1** Create request DTOs:
  - [ ] CreateVoucherRequest.java (~80 LOC)
- [ ] **2.2** Create response DTOs:
  - [ ] VoucherValidationResult.java (~40 LOC)
  - [ ] VoucherResponse.java (~30 LOC)
  - [ ] VoucherUsageResponse.java (~20 LOC)

#### Mapper (1 task)
- [ ] **2.3** Create `VoucherMapper.java` (MapStruct)

#### Service Interface (1 task)
- [ ] **2.4** Create `IVoucherService.java` (9 methods)

#### Service Implementation (3 tasks)
- [ ] **2.5** Implement `VoucherServiceImpl.java` (~500 LOC):
  - [ ] validateVoucher() with 8-step validation
  - [ ] calculateDiscount() for 4 discount types
  - [ ] CRUD operations (create, update, deactivate)
  - [ ] recordVoucherUsage() & returnVoucher()

#### Controller (1 task)
- [ ] **2.6** Create `VoucherController.java`:
  - [ ] POST /api/vouchers/validate
  - [ ] GET /api/vouchers/public
  - [ ] GET /api/vouchers/my-history
  - [ ] POST /api/vouchers/admin (ADMIN only)
  - [ ] PUT /api/vouchers/admin/{id} (ADMIN only)
  - [ ] DELETE /api/vouchers/admin/{id} (ADMIN only)

#### Testing (2 tasks)
- [ ] **2.7** Test via Postman: Create voucher (admin)
- [ ] **2.8** Test via Postman: Validate voucher
  - [ ] Valid voucher → discount calculated correctly
  - [ ] Invalid code → error message
  - [ ] Below min amount → error message
  - [ ] Exceeded user limit → error message

#### Verification (1 task)
- [ ] **2.9** Commit: `git commit -m "Day 2: Voucher service implementation"`

### Day 2 Checklist
- [ ] 8-step validation logic works correctly
- [ ] All 4 discount types calculated correctly
- [ ] Public vouchers endpoint returns active vouchers
- [ ] User history shows past usage
- [ ] Admin can create/update/deactivate vouchers
- [ ] Postman tests pass

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## 📅 Day 3-4: VNPay Integration

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### Configuration (2 tasks)
- [ ] **3.1** Create `VNPayConfig.java`
- [ ] **3.2** Update `application.yml` with VNPay settings

#### Service Implementation (4 tasks)
- [ ] **3.3** Create `VNPayService.java`:
  - [ ] createPaymentUrl() → generate signed URL
  - [ ] verifyPaymentSignature() → verify HMAC-SHA512
  - [ ] generateHMAC() → signature calculation
  - [ ] bytesToHex() → convert hash to hex string

#### Testing (3 tasks)
- [ ] **3.4** Unit test: Signature generation
- [ ] **3.5** Unit test: Signature verification
- [ ] **3.6** Generate payment URL and verify format:
  - [ ] URL contains vnp_SecureHash
  - [ ] Hash is 128 hex characters
  - [ ] All required params present

#### Sandbox Testing (1 task)
- [ ] **3.7** Test VNPay sandbox:
  - [ ] Generate payment URL
  - [ ] Complete payment with test card
  - [ ] Verify return URL
  - [ ] Check IPN callback logs

**VNPay Test Card:**
```
Bank: NCB
Card Number: 9704198526191432198
Card Name: NGUYEN VAN A
Issue Date: 07/15
OTP: 123456
```

#### Verification (1 task)
- [ ] **3.8** Commit: `git commit -m "Day 3-4: VNPay integration"`

### Day 3-4 Checklist
- [ ] VNPay configuration loaded correctly
- [ ] Signature generation algorithm correct (HMAC-SHA512)
- [ ] Signature verification works
- [ ] Payment URL generated successfully
- [ ] Sandbox payment completed
- [ ] IPN callback received

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## 📅 Day 5: Stripe Integration

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### Configuration (2 tasks)
- [ ] **5.1** Create `StripeConfig.java`
- [ ] **5.2** Update `application.yml` with Stripe settings

#### Service Implementation (3 tasks)
- [ ] **5.3** Create `StripeService.java`:
  - [ ] createCheckoutSession() → create Stripe session
  - [ ] verifyWebhookSignature() → verify HMAC-SHA256
  - [ ] retrieveSession() → get session details

#### Testing (2 tasks)
- [ ] **5.4** Unit test: Create checkout session
- [ ] **5.5** Test Stripe sandbox:
  - [ ] Create checkout session
  - [ ] Complete payment with test card: 4242 4242 4242 4242
  - [ ] Verify webhook received
  - [ ] Check webhook signature valid

#### Webhook Setup (1 task)
- [ ] **5.6** Setup Stripe CLI for local testing:
  ```bash
  stripe listen --forward-to localhost:8080/api/payments/stripe/webhook
  ```

#### Verification (1 task)
- [ ] **5.7** Commit: `git commit -m "Day 5: Stripe integration"`

### Day 5 Checklist
- [ ] Stripe SDK initialized correctly
- [ ] Checkout session created successfully
- [ ] Webhook signature verification works
- [ ] Test payment completed
- [ ] Webhook events processed

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## 📅 Day 6: PaymentService Refactoring

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### Backup (1 task)
- [ ] **6.1** Backup existing `PaymentServiceImpl.java`:
  ```bash
  cp PaymentServiceImpl.java PaymentServiceImpl.java.backup
  ```

#### DTOs (1 task)
- [ ] **6.2** Create payment DTOs:
  - [ ] CreatePaymentRequest.java
  - [ ] PaymentResponse.java

#### Service Refactoring (5 tasks)
- [ ] **6.3** Refactor `PaymentServiceImpl.java`:
  - [ ] createPaymentUrl() with voucher support
  - [ ] handleVNPayCallback() with signature verification
  - [ ] handleStripeWebhook() with signature verification
  - [ ] processSuccessfulPayment() with email
  - [ ] processFailedPayment()

#### Idempotency (1 task)
- [ ] **6.4** Implement idempotency check:
  - [ ] Redis key: `payment:idempotency:{transactionId}`
  - [ ] TTL: 24 hours
  - [ ] Prevent duplicate processing

#### Controller (1 task)
- [ ] **6.5** Create `PaymentController.java`:
  - [ ] POST /api/payments/create
  - [ ] GET /api/payments/vnpay/return
  - [ ] POST /api/payments/vnpay/ipn
  - [ ] POST /api/payments/stripe/webhook
  - [ ] GET /api/payments/verify/{transactionId}
  - [ ] POST /api/payments/cancel/{transactionId}

#### Integration Testing (3 tasks)
- [ ] **6.6** Test VNPay flow end-to-end:
  - [ ] Create booking → Create payment → Complete VNPay → IPN callback → Booking CONFIRMED
- [ ] **6.7** Test Stripe flow end-to-end:
  - [ ] Create booking → Create payment → Complete Stripe → Webhook → Booking CONFIRMED
- [ ] **6.8** Test with voucher:
  - [ ] Validate voucher → Create payment with voucher → Discount applied

#### Verification (1 task)
- [ ] **6.9** Commit: `git commit -m "Day 6: Payment service refactoring"`

### Day 6 Checklist
- [ ] Payment URL creation works (VNPay + Stripe)
- [ ] Signature verification works (both gateways)
- [ ] Idempotency prevents duplicate processing
- [ ] Voucher discount applied correctly
- [ ] Booking status updated on success
- [ ] Email sent with confirmation + QR code
- [ ] Webhook logs saved to database

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## 📅 Day 7: Testing & Documentation

**Date:** _____________  
**Time Spent:** _____ hours  
**Status:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

### Tasks

#### Unit Tests (3 tasks)
- [ ] **7.1** Create `VoucherServiceTest.java`:
  - [ ] testValidateVoucher_Success_PercentageDiscount()
  - [ ] testValidateVoucher_Fail_VoucherNotFound()
  - [ ] testValidateVoucher_Fail_BelowMinimumAmount()
  - [ ] testValidateVoucher_Fail_ExceededUserLimit()
  - [ ] testValidateVoucher_Success_MaxDiscountCap()
- [ ] **7.2** Run unit tests: `mvn test`
- [ ] **7.3** Check coverage: Should be > 80%

#### Integration Tests (2 tasks)
- [ ] **7.4** Create `PaymentIntegrationTest.java`:
  - [ ] testCreatePayment_WithVoucher_Success()
  - [ ] testValidateVoucher_Success()
- [ ] **7.5** Run integration tests: `mvn verify`

#### Manual Testing (3 tasks)
- [ ] **7.6** Test all voucher scenarios:
  - [ ] Valid voucher (PERCENTAGE)
  - [ ] Valid voucher (FIXED_AMOUNT)
  - [ ] Valid voucher (BUY_X_GET_Y)
  - [ ] Invalid code
  - [ ] Expired voucher
  - [ ] Below min amount
  - [ ] Exceeded usage limit
- [ ] **7.7** Test all payment scenarios:
  - [ ] VNPay payment without voucher
  - [ ] VNPay payment with voucher
  - [ ] Stripe payment without voucher
  - [ ] Stripe payment with voucher
  - [ ] Payment cancellation
- [ ] **7.8** Test edge cases:
  - [ ] Duplicate IPN callback → should be idempotent
  - [ ] Invalid signature → should reject
  - [ ] Concurrent payment attempts → should handle correctly

#### Documentation (2 tasks)
- [ ] **7.9** Update documentation:
  - [ ] Update `SEQUENCE_DIAGRAMS.md` with payment flows
  - [ ] Update Postman collection
  - [ ] Update `README.md` with new endpoints
- [ ] **7.10** Create deployment guide:
  - [ ] Environment variables
  - [ ] Database migration steps
  - [ ] Webhook configuration

### Day 7 Checklist
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code coverage > 80%
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Ready for code review

**Notes / Issues:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

## ✅ Acceptance Criteria (39 total)

### Payment Module (10)
- [ ] VNPay payment URL generation with HMAC-SHA512 signature
- [ ] VNPay IPN callback handling with signature verification
- [ ] Stripe checkout session creation
- [ ] Stripe webhook handling with signature verification
- [ ] Idempotency check using Redis
- [ ] Amount validation (prevent tampering)
- [ ] Transaction logging (all requests/responses)
- [ ] Booking status update on successful payment
- [ ] Email confirmation with QR code
- [ ] Payment cancellation endpoint

### Voucher Module (10)
- [ ] 8-step voucher validation logic
- [ ] Support PERCENTAGE discount type
- [ ] Support FIXED_AMOUNT discount type
- [ ] Support BUY_X_GET_Y discount type
- [ ] Usage limit enforcement (total & per user)
- [ ] Date range validation
- [ ] Applicable scope filtering (movie, theater, day, time)
- [ ] Voucher usage recording
- [ ] Voucher return on refund
- [ ] Public voucher listing

### Security (6)
- [ ] HMAC-SHA512 signature verification (VNPay)
- [ ] HMAC-SHA256 signature verification (Stripe)
- [ ] Idempotency key using Redis
- [ ] Amount validation (server-side)
- [ ] IP address logging
- [ ] Webhook signature validation

### Performance (4)
- [ ] Redis caching for idempotency check
- [ ] Async email sending (non-blocking)
- [ ] Database indexing on transaction_id, booking_id
- [ ] Webhook processing timeout (5 seconds)

### Database (4)
- [ ] Migration script V5 created
- [ ] JSON columns for gateway responses
- [ ] Foreign key constraints
- [ ] Indexes on frequently queried columns

### API Documentation (3)
- [ ] Swagger annotations on all endpoints
- [ ] Request/Response DTOs documented
- [ ] Error codes documented

### Testing (2)
- [ ] Unit tests for VoucherService
- [ ] Integration tests for Payment flow

---

## 🐛 Issues Tracker

| Issue # | Description | Severity | Status | Resolution |
|---------|-------------|----------|--------|------------|
| 1 | | 🔴 Critical / 🟡 Major / 🟢 Minor | ⬜ Open / 🟡 In Progress / ✅ Resolved | |
| 2 | | | | |
| 3 | | | | |

---

## 📈 Code Metrics

### Lines of Code
- [ ] Production Code: _____ / ~3,500 LOC
- [ ] Test Code: _____ / ~500 LOC
- [ ] Database Migration: _____ / ~200 LOC

### Files Created
- [ ] Entities: _____ / 4
- [ ] Repositories: _____ / 4
- [ ] Services: _____ / 3
- [ ] Controllers: _____ / 2
- [ ] DTOs: _____ / 6
- [ ] Config: _____ / 2
- [ ] Tests: _____ / 2

### Test Coverage
- [ ] Overall: _____% (target: > 80%)
- [ ] VoucherService: _____% (target: > 90%)
- [ ] PaymentService: _____% (target: > 85%)

---

## 🚀 Pre-Production Checklist

- [ ] All 60 tasks completed
- [ ] All 39 acceptance criteria met
- [ ] All tests pass (unit + integration)
- [ ] Code coverage > 80%
- [ ] VNPay sandbox tested successfully
- [ ] Stripe sandbox tested successfully
- [ ] Performance benchmarks met
- [ ] Security review completed
- [ ] Documentation complete
- [ ] Code reviewed and approved
- [ ] Ready for merge to `develop`

---

## 📝 Final Notes

**What went well:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

**Challenges faced:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

**Lessons learned:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

**Future improvements:**
```
_______________________________________________________
_______________________________________________________
_______________________________________________________
```

---

**Completed by:** _____________  
**Completion Date:** _____________  
**Reviewed by:** _____________  
**Review Date:** _____________  

✅ **READY FOR PRODUCTION**
