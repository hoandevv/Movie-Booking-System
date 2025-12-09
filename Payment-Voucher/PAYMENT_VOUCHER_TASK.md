# 💳 Task: Payment Gateway & Voucher System Integration

> **Branch:** `feature/payment-voucher-integration`  
> **Sprint:** Sprint 1 (Priority: CRITICAL)  
> **Assignee:** [DEV_NAME]  
> **Estimated Effort:** 5-7 days  
> **Start Date:** 2025-11-07  
> **Target Completion:** 2025-11-14

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Business Requirements](#business-requirements)
3. [Technical Requirements](#technical-requirements)
4. [Database Schema](#database-schema)
5. [API Endpoints](#api-endpoints)
6. [Implementation Tasks](#implementation-tasks)
7. [Testing Requirements](#testing-requirements)
8. [Acceptance Criteria](#acceptance-criteria)

---

## 🎯 Overview

### Problem Statement

Hiện tại hệ thống đang sử dụng **MOCK payment gateway**, dẫn đến:
- 🔴 **Security vulnerability:** Attacker có thể fake payment callback
- 🔴 **No signature verification:** Accept bất kỳ request nào
- 🔴 **No idempotency:** Có thể duplicate payment
- 🔴 **No amount validation:** User có thể thay đổi số tiền

### Solution

Implement **REAL payment gateway integration** với **Voucher/Promotion system**:
- ✅ Integrate VNPay (primary) + Stripe (secondary)
- ✅ Implement signature verification (HMAC-SHA512)
- ✅ Add idempotency with transaction ID tracking
- ✅ Validate amount, currency, và booking status
- ✅ Create webhook endpoint cho async notifications
- ✅ Add voucher/discount code support

---

## 📊 Business Requirements

### Payment Features

**1. Multiple Payment Methods**
- VNPay (Vietnam domestic)
- Stripe (International cards)
- MoMo (E-wallet) - Phase 2
- ZaloPay - Phase 2

**2. Payment Flow**
```
User → Select Seats → Create Booking (PENDING_PAYMENT)
     → Apply Voucher (optional)
     → Redirect to Payment Gateway
     → Complete Payment
     → Gateway Callback → Update Booking (CONFIRMED)
     → Send Confirmation Email với QR Code
```

**3. Payment Timeout**
- Hold seats: 120 seconds (2 minutes)
- Payment window: 900 seconds (15 minutes)
- Auto-expire nếu không thanh toán

### Voucher Features

**1. Voucher Types**
- **PERCENTAGE:** Giảm % (VD: 20% off)
- **FIXED_AMOUNT:** Giảm số tiền cố định (VD: 50,000 VND off)
- **FREE_TICKET:** Tặng 1 vé miễn phí
- **BUY_X_GET_Y:** Mua X vé được tặng Y vé

**2. Voucher Constraints**
- Minimum order amount (VD: min 200,000 VND)
- Maximum discount cap (VD: max giảm 100,000 VND)
- Usage limit per user (VD: 1 lần/user)
- Total usage limit (VD: 1000 voucher codes)
- Valid date range (start date - end date)
- Applicable movies/theaters/showtimes

**3. Voucher Application Rules**
- Validate voucher code format
- Check expiry date
- Check usage limit
- Check minimum order
- Calculate final price after discount
- Track voucher usage

---

## 🔧 Technical Requirements

### Payment Gateway Integration

**1. VNPay Configuration**
```yaml
payment:
  vnpay:
    enabled: true
    tmnCode: ${VNPAY_TMN_CODE}  # Merchant code
    hashSecret: ${VNPAY_HASH_SECRET}  # Secret key
    apiUrl: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    returnUrl: ${APP_URL}/api/payments/vnpay/return
    ipnUrl: ${APP_URL}/api/payments/vnpay/ipn  # Webhook
    version: 2.1.0
    command: pay
    currencyCode: VND
    locale: vn
```

**2. Stripe Configuration**
```yaml
payment:
  stripe:
    enabled: true
    secretKey: ${STRIPE_SECRET_KEY}
    publicKey: ${STRIPE_PUBLIC_KEY}
    webhookSecret: ${STRIPE_WEBHOOK_SECRET}
    successUrl: ${APP_URL}/booking/success
    cancelUrl: ${APP_URL}/booking/cancel
    currency: usd
```

**3. Security Requirements**
- HMAC-SHA512 signature verification cho VNPay
- HMAC-SHA256 webhook signature cho Stripe
- SSL/TLS required cho all payment endpoints
- Rate limiting: 10 requests/minute per IP
- IP whitelist cho webhook endpoints (optional)

**4. Idempotency**
- Track `transactionId` từ gateway
- Prevent duplicate payment processing
- Return cached response cho duplicate requests

**5. Error Handling**
- Gateway timeout: Retry 3 times
- Network errors: Fallback to manual verification
- Amount mismatch: Auto-cancel booking
- Signature invalid: Log security alert + reject

---

## 💾 Database Schema

### 1. Payment Transactions Table

```sql
CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    
    -- Payment Gateway Info
    gateway_type VARCHAR(20) NOT NULL,  -- VNPAY, STRIPE, MOMO
    transaction_id VARCHAR(255) NOT NULL UNIQUE,  -- Gateway txn ID
    gateway_order_id VARCHAR(255),  -- Gateway internal order ID
    
    -- Amount Info
    amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    final_amount DECIMAL(12,2) NOT NULL,  -- amount - discount
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    
    -- Status
    status VARCHAR(20) NOT NULL,  -- PENDING, SUCCESS, FAILED, REFUNDED
    payment_method VARCHAR(50),  -- VISA, MASTERCARD, ATM, etc.
    
    -- Timestamps
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    -- Gateway Response (JSON)
    gateway_request JSON,   -- Request sent to gateway
    gateway_response JSON,  -- Response from gateway
    
    -- Security
    request_signature VARCHAR(512),
    response_signature VARCHAR(512),
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    
    -- Voucher (if applied)
    voucher_id BIGINT NULL,
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_booking_id (booking_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### 2. Vouchers Table

```sql
CREATE TABLE vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Voucher Info
    code VARCHAR(50) NOT NULL UNIQUE,  -- SUMMER2024, NEWUSER20
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Discount Type
    discount_type VARCHAR(20) NOT NULL,  -- PERCENTAGE, FIXED_AMOUNT, FREE_TICKET, BUY_X_GET_Y
    discount_value DECIMAL(12,2) NOT NULL,  -- 20 (for 20%), 50000 (for 50k VND)
    
    -- Constraints
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    max_discount_amount DECIMAL(12,2) NULL,  -- Cap giảm tối đa
    
    -- Buy X Get Y (for BUY_X_GET_Y type)
    buy_quantity INT NULL,
    get_quantity INT NULL,
    
    -- Usage Limits
    total_usage_limit INT DEFAULT 1000,  -- Tổng số lần sử dụng
    usage_per_user INT DEFAULT 1,  -- Số lần mỗi user được dùng
    current_usage_count INT DEFAULT 0,  -- Đã dùng bao nhiêu lần
    
    -- Validity Period
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    
    -- Applicable Scope (NULL = apply all)
    applicable_movie_ids JSON NULL,  -- [1, 2, 3]
    applicable_theater_ids JSON NULL,
    applicable_days_of_week JSON NULL,  -- [1, 2, 3] (Monday=1)
    applicable_time_slots JSON NULL,  -- ["10:00-12:00", "18:00-22:00"]
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, EXPIRED
    is_public BOOLEAN DEFAULT TRUE,  -- Public voucher or private code
    
    -- Metadata
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_valid_period (valid_from, valid_until)
);
```

### 3. Voucher Usage Table

```sql
CREATE TABLE voucher_usages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    payment_transaction_id BIGINT NULL,
    
    -- Discount Applied
    original_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    final_amount DECIMAL(12,2) NOT NULL,
    
    -- Timestamps
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    FOREIGN KEY (user_id) REFERENCES accounts(id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id),
    
    INDEX idx_voucher_id (voucher_id),
    INDEX idx_user_id (user_id),
    INDEX idx_booking_id (booking_id),
    
    UNIQUE KEY unique_voucher_booking (voucher_id, booking_id)
);
```

### 4. Payment Webhooks Log Table

```sql
CREATE TABLE payment_webhook_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Webhook Info
    gateway_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(50),  -- payment.success, payment.failed
    
    -- Request Info
    request_headers JSON,
    request_body JSON,
    signature VARCHAR(512),
    signature_valid BOOLEAN,
    
    -- Processing
    processed BOOLEAN DEFAULT FALSE,
    processing_error TEXT NULL,
    payment_transaction_id BIGINT NULL,
    
    -- Timestamps
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    
    INDEX idx_gateway_type (gateway_type),
    INDEX idx_processed (processed),
    INDEX idx_received_at (received_at)
);
```

---

## 🌐 API Endpoints

### Payment Endpoints

#### 1. Create Payment URL
```http
POST /api/payments/create/{bookingId}
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "gateway": "VNPAY",  // or "STRIPE"
  "voucherCode": "SUMMER2024",  // optional
  "returnUrl": "https://app.com/booking/success"  // optional
}

Response 200 OK:
{
  "code": 200,
  "message": "Payment URL created successfully",
  "data": {
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
    "transactionId": "TXN_20241107_123456",
    "amount": 495000,
    "discountAmount": 99000,
    "finalAmount": 396000,
    "currency": "VND",
    "expiresAt": "2024-11-07T10:15:00",
    "voucher": {
      "code": "SUMMER2024",
      "discountType": "PERCENTAGE",
      "discountValue": 20
    }
  }
}
```

#### 2. VNPay Return (User redirect từ gateway)
```http
GET /api/payments/vnpay/return?vnp_Amount=...&vnp_BankCode=...&vnp_SecureHash=...

Response 302 Redirect:
→ {frontendUrl}/booking/success?bookingId=100&transactionId=TXN_123
→ {frontendUrl}/booking/failed?bookingId=100&reason=AMOUNT_MISMATCH
```

#### 3. VNPay IPN (Webhook - Gateway gọi server)
```http
POST /api/payments/vnpay/ipn
Content-Type: application/x-www-form-urlencoded

Request Body (form-encoded):
vnp_Amount=49500000
vnp_BankCode=NCB
vnp_TxnRef=TXN_123
vnp_ResponseCode=00
vnp_SecureHash=abc123...

Response 200 OK:
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

#### 4. Stripe Checkout Session
```http
POST /api/payments/stripe/checkout/{bookingId}
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "code": 200,
  "data": {
    "sessionId": "cs_test_abc123",
    "paymentUrl": "https://checkout.stripe.com/pay/cs_test_abc123"
  }
}
```

#### 5. Stripe Webhook
```http
POST /api/payments/stripe/webhook
Stripe-Signature: t=123,v1=abc,v2=def

Request Body (JSON):
{
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_abc123",
      "payment_intent": "pi_abc123",
      "amount_total": 3960
    }
  }
}

Response 200 OK:
{
  "received": true
}
```

#### 6. Verify Payment Status
```http
GET /api/payments/verify/{bookingId}
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "code": 200,
  "data": {
    "bookingId": 100,
    "status": "SUCCESS",
    "transactionId": "TXN_123",
    "amount": 495000,
    "finalAmount": 396000,
    "gateway": "VNPAY",
    "completedAt": "2024-11-07T10:05:30"
  }
}
```

#### 7. Cancel Payment
```http
POST /api/payments/cancel/{bookingId}
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "code": 200,
  "message": "Payment cancelled successfully. Seats released."
}
```

### Voucher Endpoints

#### 1. Validate Voucher
```http
POST /api/vouchers/validate
Authorization: Bearer {accessToken}
Content-Type: application/json

Request Body:
{
  "code": "SUMMER2024",
  "bookingId": 100
}

Response 200 OK:
{
  "code": 200,
  "message": "Voucher is valid",
  "data": {
    "voucherId": 5,
    "code": "SUMMER2024",
    "name": "Summer Sale 20%",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "minOrderAmount": 200000,
    "maxDiscountAmount": 100000,
    "originalAmount": 495000,
    "discountAmount": 99000,
    "finalAmount": 396000,
    "remainingUsage": 850,
    "validUntil": "2024-12-31T23:59:59"
  }
}

Response 400 Bad Request:
{
  "code": 400,
  "message": "Voucher validation failed",
  "errors": [
    "Voucher has expired",
    "Minimum order amount is 200,000 VND",
    "You have already used this voucher"
  ]
}
```

#### 2. Get Public Vouchers
```http
GET /api/vouchers/public?pageNumber=0&pageSize=10

Response 200 OK:
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 5,
        "code": "SUMMER2024",
        "name": "Summer Sale 20%",
        "description": "Giảm 20% cho tất cả vé",
        "discountType": "PERCENTAGE",
        "discountValue": 20,
        "minOrderAmount": 200000,
        "maxDiscountAmount": 100000,
        "validFrom": "2024-11-01T00:00:00",
        "validUntil": "2024-12-31T23:59:59",
        "remainingUsage": 850
      }
    ],
    "pageNumber": 0,
    "totalPages": 3
  }
}
```

#### 3. Get My Voucher Usage History
```http
GET /api/vouchers/my-usage?pageNumber=0&pageSize=10
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 123,
        "voucherCode": "SUMMER2024",
        "bookingId": 100,
        "originalAmount": 495000,
        "discountAmount": 99000,
        "finalAmount": 396000,
        "usedAt": "2024-11-07T10:00:00"
      }
    ]
  }
}
```

#### 4. Create Voucher (ADMIN only)
```http
POST /api/admin/vouchers
Authorization: Bearer {adminToken}
Content-Type: application/json

Request Body:
{
  "code": "BLACKFRIDAY50",
  "name": "Black Friday 50% Off",
  "description": "Giảm 50% tất cả vé dịp Black Friday",
  "discountType": "PERCENTAGE",
  "discountValue": 50,
  "minOrderAmount": 300000,
  "maxDiscountAmount": 200000,
  "totalUsageLimit": 500,
  "usagePerUser": 1,
  "validFrom": "2024-11-29T00:00:00",
  "validUntil": "2024-11-30T23:59:59",
  "isPublic": true
}

Response 200 OK:
{
  "code": 200,
  "message": "Voucher created successfully",
  "data": {
    "id": 10,
    "code": "BLACKFRIDAY50",
    ...
  }
}
```

---

## 🛠️ Implementation Tasks

### Phase 1: Database & Entities (Day 1)

**Tasks:**
- [ ] Create migration files cho 4 tables
- [ ] Create entity classes: `PaymentTransaction`, `Voucher`, `VoucherUsage`, `PaymentWebhookLog`
- [ ] Create repositories cho các entities
- [ ] Add relationships với `Booking` entity

**Files to Create:**
```
src/main/java/com/trainning/movie_booking_system/
├── entity/
│   ├── PaymentTransaction.java
│   ├── Voucher.java
│   ├── VoucherUsage.java
│   └── PaymentWebhookLog.java
├── repository/
│   ├── PaymentTransactionRepository.java
│   ├── VoucherRepository.java
│   ├── VoucherUsageRepository.java
│   └── PaymentWebhookLogRepository.java
└── untils/enums/
    ├── PaymentGateway.java (VNPAY, STRIPE, MOMO)
    ├── PaymentStatus.java (PENDING, SUCCESS, FAILED, REFUNDED)
    └── VoucherDiscountType.java (PERCENTAGE, FIXED_AMOUNT, FREE_TICKET, BUY_X_GET_Y)
```

### Phase 2: Voucher Module (Day 2)

**Tasks:**
- [ ] Create `VoucherService` interface và `VoucherServiceImpl`
- [ ] Implement voucher validation logic
- [ ] Implement discount calculation
- [ ] Create `VoucherController`
- [ ] Add DTOs: `VoucherRequest`, `VoucherResponse`, `ValidateVoucherRequest`, `ValidateVoucherResponse`

**Validation Rules:**
```java
public ValidateVoucherResponse validateVoucher(String code, Long bookingId) {
    // 1. Check voucher exists
    // 2. Check status = ACTIVE
    // 3. Check valid date range (now >= validFrom && now <= validUntil)
    // 4. Check total usage limit (currentUsageCount < totalUsageLimit)
    // 5. Check user usage limit (count user's usage < usagePerUser)
    // 6. Check minimum order amount
    // 7. Check applicable scope (movies, theaters, days, time slots)
    // 8. Calculate discount amount (respect maxDiscountAmount)
    // 9. Return final amount
}
```

**Files to Create:**
```
src/main/java/com/trainning/movie_booking_system/
├── service/
│   ├── VoucherService.java
│   └── impl/VoucherServiceImpl.java
├── controller/
│   ├── VoucherController.java
│   └── AdminVoucherController.java
├── dto/request/Voucher/
│   ├── VoucherRequest.java
│   └── ValidateVoucherRequest.java
└── dto/response/Voucher/
    ├── VoucherResponse.java
    └── ValidateVoucherResponse.java
```

### Phase 3: VNPay Integration (Day 3-4)

**Tasks:**
- [ ] Create `VNPayConfig` configuration class
- [ ] Create `VNPayService` với methods:
  - `createPaymentUrl()`
  - `verifyReturnUrl()`
  - `handleIPN()`
  - `verifySignature()`
- [ ] Create `VNPayController`
- [ ] Add HMAC-SHA512 signature generation/verification
- [ ] Test với VNPay sandbox

**Signature Generation:**
```java
public String generateVNPaySignature(Map<String, String> params, String secretKey) {
    // 1. Sort params by key (alphabetically)
    TreeMap<String, String> sortedParams = new TreeMap<>(params);
    
    // 2. Build sign data: key1=value1&key2=value2
    String signData = sortedParams.entrySet().stream()
        .filter(e -> !e.getKey().equals("vnp_SecureHash"))
        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8))
        .collect(Collectors.joining("&"));
    
    // 3. Calculate HMAC-SHA512
    Mac sha512 = Mac.getInstance("HmacSHA512");
    SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");
    sha512.init(secretKeySpec);
    byte[] hash = sha512.doFinal(signData.getBytes());
    
    // 4. Convert to hex string
    return bytesToHex(hash);
}
```

**Files to Create:**
```
src/main/java/com/trainning/movie_booking_system/
├── config/
│   └── VNPayConfig.java
├── service/
│   ├── VNPayService.java
│   └── impl/VNPayServiceImpl.java
├── controller/
│   └── VNPayController.java
├── dto/request/Payment/
│   └── VNPayReturnRequest.java
└── dto/response/Payment/
    └── VNPayIPNResponse.java
```

### Phase 4: Stripe Integration (Day 5)

**Tasks:**
- [ ] Add Stripe dependency to `pom.xml`
- [ ] Create `StripeConfig`
- [ ] Create `StripeService` với methods:
  - `createCheckoutSession()`
  - `handleWebhook()`
  - `verifyWebhookSignature()`
- [ ] Create `StripeController`
- [ ] Test với Stripe test mode

**Files to Create:**
```
src/main/java/com/trainning/movie_booking_system/
├── config/
│   └── StripeConfig.java
├── service/
│   ├── StripeService.java
│   └── impl/StripeServiceImpl.java
└── controller/
    └── StripeController.java
```

### Phase 5: Refactor PaymentService (Day 6)

**Tasks:**
- [ ] Refactor `PaymentServiceImpl` để integrate với VNPay & Stripe
- [ ] Add voucher support trong payment flow
- [ ] Create `PaymentTransaction` entity khi payment initiated
- [ ] Update `PaymentTransaction` khi payment completed/failed
- [ ] Send email confirmation với QR code
- [ ] Add idempotency check

**Updated Flow:**
```java
public CreatePaymentResponse createPaymentUrl(CreatePaymentRequest request) {
    // 1. Load booking
    Booking booking = bookingRepository.findById(request.getBookingId())...;
    
    // 2. Validate booking status = PENDING_PAYMENT
    
    // 3. Apply voucher if provided
    BigDecimal finalAmount = booking.getTotalPrice();
    Voucher voucher = null;
    if (request.getVoucherCode() != null) {
        ValidateVoucherResponse validation = voucherService.validate(
            request.getVoucherCode(), booking.getId()
        );
        finalAmount = validation.getFinalAmount();
        voucher = voucherRepository.findByCode(request.getVoucherCode())...;
    }
    
    // 4. Create PaymentTransaction (status = PENDING)
    PaymentTransaction transaction = PaymentTransaction.builder()
        .booking(booking)
        .gatewayType(request.getGateway())
        .transactionId(generateTransactionId())
        .amount(booking.getTotalPrice())
        .discountAmount(voucher != null ? validation.getDiscountAmount() : 0)
        .finalAmount(finalAmount)
        .currency("VND")
        .status(PaymentStatus.PENDING)
        .voucher(voucher)
        .build();
    paymentTransactionRepository.save(transaction);
    
    // 5. Generate payment URL based on gateway
    String paymentUrl;
    if (request.getGateway() == PaymentGateway.VNPAY) {
        paymentUrl = vnpayService.createPaymentUrl(transaction);
    } else if (request.getGateway() == PaymentGateway.STRIPE) {
        paymentUrl = stripeService.createCheckoutSession(transaction);
    }
    
    // 6. Return response
    return CreatePaymentResponse.builder()
        .paymentUrl(paymentUrl)
        .transactionId(transaction.getTransactionId())
        .finalAmount(finalAmount)
        .build();
}
```

### Phase 6: Testing & Documentation (Day 7)

**Tasks:**
- [ ] Write unit tests cho VoucherService
- [ ] Write integration tests cho Payment flow
- [ ] Test VNPay sandbox với test cards
- [ ] Test Stripe test mode
- [ ] Update Postman collection
- [ ] Update API documentation
- [ ] Update sequence diagrams

---

## 🧪 Testing Requirements

### Unit Tests

**VoucherServiceTest:**
```java
@Test
void validateVoucher_Success() {
    // Given: Valid voucher + valid booking
    // When: validateVoucher()
    // Then: Return validation response with discount
}

@Test
void validateVoucher_Expired() {
    // Given: Expired voucher
    // When: validateVoucher()
    // Then: Throw BadRequestException("Voucher has expired")
}

@Test
void validateVoucher_UsageLimitExceeded() {
    // Given: Voucher with usage limit reached
    // When: validateVoucher()
    // Then: Throw BadRequestException("Voucher usage limit exceeded")
}

@Test
void calculateDiscount_Percentage() {
    // Given: 20% voucher, order 500,000 VND
    // When: calculateDiscount()
    // Then: discount = 100,000 VND
}

@Test
void calculateDiscount_Percentage_WithCap() {
    // Given: 20% voucher, max discount 50,000 VND, order 500,000 VND
    // When: calculateDiscount()
    // Then: discount = 50,000 VND (capped)
}
```

**VNPayServiceTest:**
```java
@Test
void generateSignature_Success() {
    // Given: Params map + secret key
    // When: generateSignature()
    // Then: Return valid HMAC-SHA512 hash
}

@Test
void verifySignature_Valid() {
    // Given: Valid params with correct signature
    // When: verifySignature()
    // Then: Return true
}

@Test
void verifySignature_Invalid() {
    // Given: Tampered params
    // When: verifySignature()
    // Then: Return false
}
```

### Integration Tests

**Payment Flow E2E:**
```java
@Test
void completePaymentFlow_VNPay_Success() {
    // 1. Create booking
    // 2. Create payment URL
    // 3. Simulate VNPay callback with valid signature
    // 4. Verify booking status = CONFIRMED
    // 5. Verify payment transaction created
    // 6. Verify seats consumed
    // 7. Verify email sent
}

@Test
void completePaymentFlow_WithVoucher_Success() {
    // 1. Create booking (500,000 VND)
    // 2. Apply voucher (20% off)
    // 3. Create payment URL (final: 400,000 VND)
    // 4. Simulate callback with 400,000 VND
    // 5. Verify discount applied
    // 6. Verify voucher usage recorded
}

@Test
void paymentCallback_AmountMismatch_Failed() {
    // 1. Create payment (expected: 500,000 VND)
    // 2. Simulate callback with 300,000 VND
    // 3. Verify booking status = CANCELLED
    // 4. Verify seats released
}

@Test
void paymentCallback_InvalidSignature_Rejected() {
    // 1. Create payment
    // 2. Simulate callback with invalid signature
    // 3. Verify SecurityException thrown
    // 4. Verify webhook log created with signature_valid=false
}
```

### Manual Testing

**VNPay Sandbox Test Cards:**
```
Bank: NCB
Card Number: 9704198526191432198
Cardholder: NGUYEN VAN A
Expiry: 07/15
OTP: 123456
```

**Stripe Test Cards:**
```
Success: 4242 4242 4242 4242
Decline: 4000 0000 0000 0002
3D Secure: 4000 0027 6000 3184
```

---

## ✅ Acceptance Criteria

### Payment Module

- [ ] ✅ VNPay payment URL tạo thành công với signature đúng
- [ ] ✅ VNPay IPN webhook verify signature chính xác
- [ ] ✅ Stripe checkout session tạo thành công
- [ ] ✅ Stripe webhook verify signature chính xác
- [ ] ✅ Payment transaction được lưu với đầy đủ thông tin
- [ ] ✅ Idempotency check hoạt động (duplicate callback → return cached response)
- [ ] ✅ Amount validation hoạt động (mismatch → cancel booking)
- [ ] ✅ Payment timeout hoạt động (15 phút → auto-expire)
- [ ] ✅ Seats được consume sau payment success
- [ ] ✅ Email confirmation gửi với QR code

### Voucher Module

- [ ] ✅ Voucher validation hoạt động đúng với tất cả rules
- [ ] ✅ Discount calculation chính xác cho PERCENTAGE type
- [ ] ✅ Discount calculation chính xác cho FIXED_AMOUNT type
- [ ] ✅ Max discount cap được respect
- [ ] ✅ Min order amount được check
- [ ] ✅ Usage limit per user được enforce
- [ ] ✅ Total usage limit được enforce
- [ ] ✅ Voucher usage được track trong database
- [ ] ✅ Public vouchers list API hoạt động
- [ ] ✅ Admin có thể tạo/update/delete vouchers

### Security

- [ ] ✅ Signature verification cho VNPay hoạt động
- [ ] ✅ Signature verification cho Stripe webhook hoạt động
- [ ] ✅ Không thể fake payment callback
- [ ] ✅ Rate limiting hoạt động (10 req/min)
- [ ] ✅ Sensitive data không log ra console
- [ ] ✅ Webhook logs được lưu cho audit

### Performance

- [ ] ✅ Payment URL generation < 500ms
- [ ] ✅ Webhook processing < 1000ms
- [ ] ✅ Voucher validation < 200ms
- [ ] ✅ No N+1 queries trong payment flow

---

## 📊 Success Metrics

**Before Implementation:**
- 🔴 Payment gateway: MOCK
- 🔴 Security: 0/10 (no signature verification)
- 🔴 Voucher system: Not exist
- 🔴 Production ready: NO

**After Implementation:**
- ✅ Payment gateway: VNPay + Stripe integrated
- ✅ Security: 9/10 (signature verification, idempotency, amount validation)
- ✅ Voucher system: Full-featured
- ✅ Production ready: YES

---

## 🚀 Next Steps After Completion

1. **Phase 2 Payment Methods:**
   - MoMo integration
   - ZaloPay integration
   - Bank transfer (manual confirmation)

2. **Advanced Voucher Features:**
   - Referral codes
   - Birthday vouchers (auto-send)
   - Loyalty points system
   - Combo deals

3. **Analytics:**
   - Payment success rate tracking
   - Voucher usage analytics
   - Revenue by payment method
   - Discount impact analysis

---

**🎯 LET'S BUILD IT!** 💪

