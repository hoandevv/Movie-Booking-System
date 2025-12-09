# ✅ Payment & Voucher - Daily Checklist

**Print this file and check off tasks as you complete them!**

---

## 📅 DAY 1: Database & Entities (4-6h)

### Morning (2-3h)
- [ ] Add Stripe, Commons Codec, Gson to `pom.xml`
- [ ] Run `mvn clean install` → ✅ SUCCESS
- [ ] Create `V5__create_payment_voucher_tables.sql`
- [ ] Run `mvn flyway:migrate`
- [ ] Verify: `SHOW TABLES;` → See 4 new tables

### Afternoon (2-3h)
- [ ] Create 4 enums in `utils/enums/`
- [ ] Create 4 entities in `entity/`
- [ ] Create 4 repositories in `repository/`
- [ ] Run `mvn clean compile` → ✅ SUCCESS
- [ ] Commit: `git commit -m "Day 1: Database schema & entities"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📅 DAY 2: Voucher Service (6-8h)

### Morning (3-4h)
- [ ] Create DTOs: CreateVoucherRequest, VoucherResponse, VoucherValidationResult
- [ ] Create `VoucherMapper.java` (MapStruct)
- [ ] Create `IVoucherService.java` interface

### Afternoon (3-4h)
- [ ] Implement `VoucherServiceImpl.java` (~500 LOC)
  - [ ] validateVoucher() - 8 steps
  - [ ] calculateDiscount() - 4 types
  - [ ] CRUD methods
- [ ] Create `VoucherController.java`
- [ ] Test: Create voucher via Postman (ADMIN)
- [ ] Test: Validate voucher via Postman
- [ ] Commit: `git commit -m "Day 2: Voucher service"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📅 DAY 3-4: VNPay Integration (8-12h)

### Day 3 Morning (3-4h)
- [ ] Create `VNPayConfig.java`
- [ ] Update `application.yml` with VNPay config
- [ ] Create `VNPayService.java` - Part 1
  - [ ] generateHMAC() method
  - [ ] bytesToHex() method

### Day 3 Afternoon (3-4h)
- [ ] `VNPayService.java` - Part 2
  - [ ] createPaymentUrl() method
  - [ ] verifyPaymentSignature() method
- [ ] Test: Generate payment URL
- [ ] Verify: URL contains vnp_SecureHash (128 hex chars)

### Day 4 (2-4h)
- [ ] Test VNPay sandbox
  - [ ] Card: 9704198526191432198
  - [ ] OTP: 123456
- [ ] Verify IPN callback logs
- [ ] Commit: `git commit -m "Day 3-4: VNPay integration"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📅 DAY 5: Stripe Integration (6-8h)

### Morning (3-4h)
- [ ] Create `StripeConfig.java`
- [ ] Update `application.yml` with Stripe config
- [ ] Create `StripeService.java`
  - [ ] createCheckoutSession()
  - [ ] verifyWebhookSignature()
  - [ ] retrieveSession()

### Afternoon (3-4h)
- [ ] Install Stripe CLI: `stripe listen --forward-to localhost:8080/api/payments/stripe/webhook`
- [ ] Test: Create checkout session
- [ ] Test: Pay with card 4242 4242 4242 4242
- [ ] Verify: Webhook received
- [ ] Commit: `git commit -m "Day 5: Stripe integration"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📅 DAY 6: PaymentService Refactoring (8-10h)

### Morning (4-5h)
- [ ] Backup: `cp PaymentServiceImpl.java PaymentServiceImpl.java.backup`
- [ ] Create DTOs: CreatePaymentRequest, PaymentResponse
- [ ] Refactor `PaymentServiceImpl.java` - Part 1
  - [ ] createPaymentUrl() with voucher
  - [ ] Idempotency check (Redis)

### Afternoon (4-5h)
- [ ] Refactor `PaymentServiceImpl.java` - Part 2
  - [ ] handleVNPayCallback()
  - [ ] handleStripeWebhook()
  - [ ] processSuccessfulPayment()
  - [ ] processFailedPayment()
- [ ] Create `PaymentController.java` (7 endpoints)
- [ ] Test end-to-end: Booking → Payment → Confirm
- [ ] Commit: `git commit -m "Day 6: Payment service refactoring"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📅 DAY 7: Testing & Documentation (6-8h)

### Morning (3-4h)
- [ ] Create `VoucherServiceTest.java` (8 test cases)
- [ ] Create `PaymentIntegrationTest.java`
- [ ] Run: `mvn test` → ✅ All pass
- [ ] Run: `mvn verify` → ✅ All pass
- [ ] Check coverage: _____ % (target: > 80%)

### Afternoon (3-4h)
- [ ] Manual testing: All voucher scenarios
- [ ] Manual testing: All payment scenarios
- [ ] Update `SEQUENCE_DIAGRAMS.md`
- [ ] Update Postman collection
- [ ] Update `README.md`
- [ ] Commit: `git commit -m "Day 7: Testing & documentation"`

**Time Spent:** _____ hours  
**Status:** ⬜ Pending | 🟡 In Progress | ✅ Complete

---

## 📊 Acceptance Criteria Tracker

### Payment (10/10)
- [ ] VNPay URL generation (HMAC-SHA512)
- [ ] VNPay IPN callback
- [ ] Stripe checkout session
- [ ] Stripe webhook
- [ ] Idempotency (Redis)
- [ ] Amount validation
- [ ] Transaction logging
- [ ] Booking status update
- [ ] Email with QR code
- [ ] Payment cancellation

### Voucher (10/10)
- [ ] 8-step validation
- [ ] PERCENTAGE discount
- [ ] FIXED_AMOUNT discount
- [ ] BUY_X_GET_Y discount
- [ ] Usage limits
- [ ] Date range validation
- [ ] Scope filtering
- [ ] Usage recording
- [ ] Voucher return
- [ ] Public listing

### Security (6/6)
- [ ] VNPay signature (HMAC-SHA512)
- [ ] Stripe signature (HMAC-SHA256)
- [ ] Idempotency key
- [ ] Amount validation
- [ ] IP logging
- [ ] Webhook validation

### Performance (4/4)
- [ ] Redis caching
- [ ] Async email
- [ ] DB indexing
- [ ] Webhook timeout

### Database (4/4)
- [ ] Migration V5
- [ ] JSON columns
- [ ] Foreign keys
- [ ] Indexes

### API Docs (3/3)
- [ ] Swagger annotations
- [ ] DTOs documented
- [ ] Error codes

### Testing (2/2)
- [ ] Unit tests
- [ ] Integration tests

**Total:** _____ / 39 (_____ %)

---

## 🎯 Final Pre-Production Checklist

- [ ] All 60 tasks complete
- [ ] All 39 acceptance criteria met
- [ ] All tests pass
- [ ] Coverage > 80%
- [ ] VNPay sandbox ✅
- [ ] Stripe sandbox ✅
- [ ] Performance OK
- [ ] Security review ✅
- [ ] Documentation ✅
- [ ] Code reviewed ✅
- [ ] Ready for merge

---

## 📝 Quick Notes

**Today's Focus:**
```
___________________________________________________
___________________________________________________
```

**Blockers:**
```
___________________________________________________
___________________________________________________
```

**Tomorrow's Plan:**
```
___________________________________________________
___________________________________________________
```

---

**Started:** _____ / _____ / _____  
**Completed:** _____ / _____ / _____  
**Total Time:** _____ hours
