# 🛠️ Payment & Voucher Implementation Guide

> Step-by-step implementation guide với code examples

---

## 📋 Implementation Checklist

### Day 1: Database & Entities ✅
- [ ] Create migration files
- [ ] Create entity classes
- [ ] Create repositories
- [ ] Add enums

### Day 2: Voucher Module ✅
- [ ] VoucherService implementation
- [ ] Voucher validation logic
- [ ] VoucherController
- [ ] DTOs

### Day 3-4: VNPay Integration ✅
- [ ] VNPayConfig
- [ ] VNPayService
- [ ] Signature generation/verification
- [ ] VNPayController

### Day 5: Stripe Integration ✅
- [ ] Add Stripe dependency
- [ ] StripeConfig
- [ ] StripeService
- [ ] StripeController

### Day 6: Refactor PaymentService ✅
- [ ] Integrate VNPay & Stripe
- [ ] Add voucher support
- [ ] Idempotency check
- [ ] Email confirmation

### Day 7: Testing ✅
- [ ] Unit tests
- [ ] Integration tests
- [ ] Sandbox testing
- [ ] Documentation

---

## 📦 Step 1: Add Dependencies

### pom.xml

```xml
<!-- Add to dependencies section -->

<!-- Stripe Payment Gateway -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.16.0</version>
</dependency>

<!-- Apache Commons Codec for HMAC -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.16.0</version>
</dependency>

<!-- JSON parsing for payment gateway responses -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

---

## 🗃️ Step 2: Create Database Migrations

### V5__create_payment_voucher_tables.sql

```sql
-- Migration file: src/main/resources/db/migration/V5__create_payment_voucher_tables.sql

-- 1. Payment Transactions Table
CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    
    -- Payment Gateway Info
    gateway_type VARCHAR(20) NOT NULL COMMENT 'VNPAY, STRIPE, MOMO',
    transaction_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique transaction ID',
    gateway_order_id VARCHAR(255) COMMENT 'Gateway internal order ID',
    
    -- Amount Info
    amount DECIMAL(12,2) NOT NULL COMMENT 'Original booking amount',
    discount_amount DECIMAL(12,2) DEFAULT 0 COMMENT 'Discount from voucher',
    final_amount DECIMAL(12,2) NOT NULL COMMENT 'Final amount to charge',
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    
    -- Status
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, SUCCESS, FAILED, REFUNDED',
    payment_method VARCHAR(50) COMMENT 'VISA, MASTERCARD, ATM, etc.',
    
    -- Timestamps
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    
    -- Gateway Response (JSON)
    gateway_request JSON COMMENT 'Request sent to gateway',
    gateway_response JSON COMMENT 'Response from gateway',
    
    -- Security
    request_signature VARCHAR(512) COMMENT 'Signature sent to gateway',
    response_signature VARCHAR(512) COMMENT 'Signature from gateway',
    ip_address VARCHAR(45) COMMENT 'User IP address',
    user_agent VARCHAR(512) COMMENT 'User browser/device',
    
    -- Voucher (if applied)
    voucher_id BIGINT NULL,
    
    -- Refund
    refund_id VARCHAR(255) NULL COMMENT 'Refund transaction ID',
    refund_reason TEXT NULL,
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_booking_id (booking_id),
    INDEX idx_status (status),
    INDEX idx_gateway_type (gateway_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Vouchers Table
CREATE TABLE vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Voucher Info
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Voucher code (e.g., SUMMER2024)',
    name VARCHAR(255) NOT NULL COMMENT 'Display name',
    description TEXT COMMENT 'Voucher description',
    
    -- Discount Type
    discount_type VARCHAR(20) NOT NULL COMMENT 'PERCENTAGE, FIXED_AMOUNT, FREE_TICKET, BUY_X_GET_Y',
    discount_value DECIMAL(12,2) NOT NULL COMMENT 'Discount value (20 for 20%, 50000 for 50k VND)',
    
    -- Constraints
    min_order_amount DECIMAL(12,2) DEFAULT 0 COMMENT 'Minimum order amount to use voucher',
    max_discount_amount DECIMAL(12,2) NULL COMMENT 'Maximum discount cap',
    
    -- Buy X Get Y (for BUY_X_GET_Y type)
    buy_quantity INT NULL COMMENT 'Number of tickets to buy',
    get_quantity INT NULL COMMENT 'Number of free tickets',
    
    -- Usage Limits
    total_usage_limit INT DEFAULT 1000 COMMENT 'Total number of times voucher can be used',
    usage_per_user INT DEFAULT 1 COMMENT 'Max usage per user',
    current_usage_count INT DEFAULT 0 COMMENT 'Current number of times used',
    
    -- Validity Period
    valid_from TIMESTAMP NOT NULL COMMENT 'Voucher valid from date',
    valid_until TIMESTAMP NOT NULL COMMENT 'Voucher expiry date',
    
    -- Applicable Scope (NULL = apply all)
    applicable_movie_ids JSON NULL COMMENT 'Array of movie IDs [1, 2, 3]',
    applicable_theater_ids JSON NULL COMMENT 'Array of theater IDs',
    applicable_days_of_week JSON NULL COMMENT 'Days: [1=Mon, 2=Tue, ...]',
    applicable_time_slots JSON NULL COMMENT 'Time ranges: ["10:00-12:00", "18:00-22:00"]',
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, EXPIRED',
    is_public BOOLEAN DEFAULT TRUE COMMENT 'Public or private voucher',
    
    -- Metadata
    created_by BIGINT COMMENT 'Admin user ID who created voucher',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_valid_period (valid_from, valid_until),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Voucher Usage Table
CREATE TABLE voucher_usages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    payment_transaction_id BIGINT NULL,
    
    -- Discount Applied
    original_amount DECIMAL(12,2) NOT NULL COMMENT 'Original booking amount',
    discount_amount DECIMAL(12,2) NOT NULL COMMENT 'Discount applied',
    final_amount DECIMAL(12,2) NOT NULL COMMENT 'Final amount after discount',
    
    -- Timestamps
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id) ON DELETE SET NULL,
    
    INDEX idx_voucher_id (voucher_id),
    INDEX idx_user_id (user_id),
    INDEX idx_booking_id (booking_id),
    INDEX idx_used_at (used_at),
    
    UNIQUE KEY unique_voucher_booking (voucher_id, booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Payment Webhook Logs Table
CREATE TABLE payment_webhook_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Webhook Info
    gateway_type VARCHAR(20) NOT NULL COMMENT 'VNPAY, STRIPE, MOMO',
    event_type VARCHAR(50) COMMENT 'payment.success, payment.failed, checkout.session.completed',
    
    -- Request Info
    request_headers JSON COMMENT 'HTTP headers',
    request_body JSON COMMENT 'Webhook payload',
    signature VARCHAR(512) COMMENT 'Webhook signature',
    signature_valid BOOLEAN COMMENT 'Signature verification result',
    
    -- Processing
    processed BOOLEAN DEFAULT FALSE COMMENT 'Whether webhook has been processed',
    processing_error TEXT NULL COMMENT 'Error message if processing failed',
    payment_transaction_id BIGINT NULL COMMENT 'Associated payment transaction',
    
    -- Timestamps
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    
    INDEX idx_gateway_type (gateway_type),
    INDEX idx_processed (processed),
    INDEX idx_signature_valid (signature_valid),
    INDEX idx_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Insert sample vouchers for testing
INSERT INTO vouchers (code, name, description, discount_type, discount_value, min_order_amount, max_discount_amount, total_usage_limit, usage_per_user, valid_from, valid_until, status) VALUES
('SUMMER2024', 'Summer Sale 20%', 'Giảm 20% cho tất cả vé trong mùa hè', 'PERCENTAGE', 20, 200000, 100000, 1000, 1, '2024-06-01 00:00:00', '2024-08-31 23:59:59', 'ACTIVE'),
('WELCOME50K', 'Welcome Discount', 'Giảm 50,000 VND cho khách hàng mới', 'FIXED_AMOUNT', 50000, 150000, NULL, 500, 1, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ACTIVE'),
('BUY2GET1', 'Buy 2 Get 1 Free', 'Mua 2 vé tặng 1 vé', 'BUY_X_GET_Y', 0, 0, NULL, 200, 2, '2024-11-01 00:00:00', '2024-11-30 23:59:59', 'ACTIVE');
```

---

## 🏗️ Step 3: Create Enums

### PaymentGateway.java

```java
package com.trainning.movie_booking_system.utils.enums;

public enum PaymentGateway {
    VNPAY("VNPay"),
    STRIPE("Stripe"),
    MOMO("MoMo"),
    ZALOPAY("ZaloPay");

    private final String displayName;

    PaymentGateway(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### PaymentStatus.java

```java
package com.trainning.movie_booking_system.utils.enums;

public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    SUCCESS("Thành công"),
    FAILED("Thất bại"),
    REFUNDED("Đã hoàn tiền"),
    CANCELLED("Đã hủy");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### VoucherDiscountType.java

```java
package com.trainning.movie_booking_system.utils.enums;

public enum VoucherDiscountType {
    PERCENTAGE("Giảm theo phần trăm"),
    FIXED_AMOUNT("Giảm số tiền cố định"),
    FREE_TICKET("Tặng vé miễn phí"),
    BUY_X_GET_Y("Mua X tặng Y");

    private final String displayName;

    VoucherDiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### VoucherStatus.java

```java
package com.trainning.movie_booking_system.utils.enums;

public enum VoucherStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Tạm ngừng"),
    EXPIRED("Đã hết hạn");

    private final String displayName;

    VoucherStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 📦 Step 4: Create Entities

### PaymentTransaction.java

```java
package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.PaymentGateway;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 20)
    private PaymentGateway gatewayType;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private LocalDateTime initiatedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_request", columnDefinition = "json")
    private Map<String, Object> gatewayRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "json")
    private Map<String, Object> gatewayResponse;

    @Column(name = "request_signature", length = 512)
    private String requestSignature;

    @Column(name = "response_signature", length = 512)
    private String responseSignature;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "refund_id")
    private String refundId;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Voucher.java

```java
package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.VoucherDiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private VoucherDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @Column(name = "total_usage_limit")
    @Builder.Default
    private Integer totalUsageLimit = 1000;

    @Column(name = "usage_per_user")
    @Builder.Default
    private Integer usagePerUser = 1;

    @Column(name = "current_usage_count")
    @Builder.Default
    private Integer currentUsageCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_movie_ids", columnDefinition = "json")
    private List<Long> applicableMovieIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_theater_ids", columnDefinition = "json")
    private List<Long> applicableTheaterIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_days_of_week", columnDefinition = "json")
    private List<Integer> applicableDaysOfWeek;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_time_slots", columnDefinition = "json")
    private List<String> applicableTimeSlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == VoucherStatus.ACTIVE
                && !now.isBefore(validFrom)
                && !now.isAfter(validUntil)
                && currentUsageCount < totalUsageLimit;
    }

    public void incrementUsage() {
        this.currentUsageCount++;
    }

    public void decrementUsage() {
        if (this.currentUsageCount > 0) {
            this.currentUsageCount--;
        }
    }
}
```

### VoucherUsage.java

```java
package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Account user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "used_at", nullable = false)
    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
}
```

### PaymentWebhookLog.java

```java
package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.PaymentGateway;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment_webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 20)
    private PaymentGateway gatewayType;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "json")
    private Map<String, String> requestHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "json")
    private Map<String, Object> requestBody;

    @Column(name = "signature", length = 512)
    private String signature;

    @Column(name = "signature_valid")
    private Boolean signatureValid;

    @Column(name = "processed")
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
```

---

## 📚 Step 5: Create Repositories

### PaymentTransactionRepository.java

```java
package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.PaymentTransaction;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findByBookingId(Long bookingId);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.booking.id = :bookingId AND pt.status = :status")
    Optional<PaymentTransaction> findByBookingIdAndStatus(
            @Param("bookingId") Long bookingId,
            @Param("status") PaymentStatus status
    );

    boolean existsByTransactionId(String transactionId);

    boolean existsByGatewayOrderId(String gatewayOrderId);
}
```

### VoucherRepository.java

```java
package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    @Query("SELECT v FROM Voucher v WHERE v.status = :status AND v.isPublic = true " +
           "AND v.validFrom <= :now AND v.validUntil >= :now " +
           "AND v.currentUsageCount < v.totalUsageLimit")
    Page<Voucher> findActivePublicVouchers(
            @Param("status") VoucherStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    boolean existsByCode(String code);
}
```

### VoucherUsageRepository.java

```java
package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.VoucherUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId AND vu.user.id = :userId")
    long countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);

    Optional<VoucherUsage> findByBookingId(Long bookingId);

    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.user.id = :userId ORDER BY vu.usedAt DESC")
    Page<VoucherUsage> findByUserId(@Param("userId") Long userId, Pageable pageable);

    void deleteByBookingId(Long bookingId);
}
```

### PaymentWebhookLogRepository.java

```java
package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.PaymentWebhookLog;
import com.trainning.movie_booking_system.utils.enums.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.processed = false " +
           "AND pwl.receivedAt > :since ORDER BY pwl.receivedAt ASC")
    List<PaymentWebhookLog> findUnprocessedWebhooks(@Param("since") LocalDateTime since);

    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.gatewayType = :gateway " +
           "AND pwl.signatureValid = false")
    List<PaymentWebhookLog> findInvalidSignatureWebhooks(@Param("gateway") PaymentGateway gateway);
}
```

---

**📝 Note:** Phần này chỉ là nửa đầu của Implementation Guide. Tiếp tục với:
- Step 6: VoucherService Implementation
- Step 7: VNPayService Implementation
- Step 8: StripeService Implementation
- Step 9: PaymentService Refactoring
- Step 10: Controllers
- Step 11: Testing

Bạn muốn tôi tiếp tục với các phần còn lại không?

