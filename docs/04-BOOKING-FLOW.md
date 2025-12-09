# 🎬 Booking Flow - Complete Guide

> **Status:** ✅ Implemented & Fixed  
> **Last Updated:** November 11, 2025  
> **Related Issues:** Fixed critical race conditions & expiration logic

---

## 📋 OVERVIEW

Booking flow cho phép user chọn ghế, tạo booking và thanh toán trong vòng **15 phút**.

### **Key Features:**
- ✅ **Concurrency Control** - Distributed locks để tránh double booking
- ✅ **TOCTOU Prevention** - Re-verify holds under lock
- ✅ **Auto-Expiration** - Cron job expire bookings sau 15 phút
- ✅ **Seat Hold** - Redis holds với TTL 120s

---

## 🔄 COMPLETE FLOW

```
1. User chọn ghế → Hold Seats (120s)
   ↓
2. User confirm → Create Booking (PENDING_PAYMENT, expires in 15min)
   ↓
3. User thanh toán → Payment Success
   ↓
4. Booking CONFIRMED
   
   HOẶC
   
5. Không thanh toán → Auto-expire sau 15 phút (Cron job)
```

---

## 🚀 API ENDPOINTS

### 1. Hold Seats

**Endpoint:** `POST /api/v1/seats/hold`

**Request:**
```json
{
  "showtimeId": 10,
  "seatIds": [1, 2, 3],
  "ttlSec": 120
}
```

**Response:**
```json
{
  "success": true,
  "message": "Seats held for 120 seconds",
  "data": {
    "expiresAt": "2024-11-11T10:02:00"
  }
}
```

**Business Rules:**
- TTL mặc định: 120 giây
- Seat đã held bởi user khác → 409 Conflict
- Seat đã held bởi chính user → Refresh TTL (idempotent)
- Showtime đã bắt đầu → 400 Bad Request

---

### 2. Create Booking

**Endpoint:** `POST /api/v1/bookings`

**Request:**
```json
{
  "showtimeId": 10,
  "seatIds": [1, 2, 3]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Booking created successfully. Please complete payment within 15 minutes.",
  "data": {
    "id": 100,
    "showtimeId": 10,
    "totalPrice": 495000,
    "status": "PENDING_PAYMENT",
    "expiresAt": "2024-11-11T10:15:00",
    "seats": [
      {"seatId": 1, "type": "VIP", "price": 195000},
      {"seatId": 2, "type": "STANDARD", "price": 150000},
      {"seatId": 3, "type": "STANDARD", "price": 150000}
    ]
  }
}
```

**Business Rules:**
- ✅ Validate showtime chưa bắt đầu
- ✅ Validate cutoff time (15 phút trước showtime)
- ✅ Verify seats held by current user
- ✅ Acquire distributed locks (sorted order)
- ✅ Re-verify holds under lock (TOCTOU prevention)
- ✅ Check DB seats chưa booked
- ✅ Calculate prices (VIP = base * 1.3)
- ✅ Set `expiresAt = now + 15 minutes`
- ✅ Consume Redis holds
- ✅ Release locks

**Errors:**
- `400` - Showtime already started
- `400` - Booking closes 15 minutes before showtime
- `409` - Seat not held or held by another user
- `409` - Cannot lock seat (concurrent request)
- `409` - Seat already booked in DB (race condition)

---

### 3. Get Booking Details

**Endpoint:** `GET /api/v1/bookings/{bookingId}`

**Response:**
```json
{
  "id": 100,
  "status": "PENDING_PAYMENT",
  "totalPrice": 495000,
  "expiresAt": "2024-11-11T10:15:00",
  "showtime": {
    "id": 10,
    "movieTitle": "Spider-Man",
    "startTime": "2024-11-11T20:00:00",
    "theater": "CGV Vincom",
    "screen": "Screen 1"
  },
  "seats": [...]
}
```

---

### 4. Cancel Booking

**Endpoint:** `DELETE /api/v1/bookings/{bookingId}/payment`

**Response:**
```json
{
  "success": true,
  "message": "Payment cancelled successfully. Seats are now available."
}
```

**Business Rules:**
- Chỉ cancel được booking PENDING_PAYMENT
- Update status = CANCELLED
- Release seats

---

## ⏰ AUTO-EXPIRATION

### **Cron Job Configuration:**

```java
@Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
public void expireBookings() {
    // Find bookings: status=PENDING_PAYMENT AND expiresAt < NOW()
    // Update status = EXPIRED
    // Cancel payment transactions
    // Release seat holds
}
```

### **Expiration Timeline:**

```
10:00 - User creates booking (expiresAt = 10:15)
10:05 - Cron runs (no expired)
10:10 - Cron runs (no expired)
10:15 - Cron runs → Booking EXPIRED → Seats released
```

---

## 🔒 CONCURRENCY CONTROL

### **Mechanisms:**

1. **Redis SETNX (Seat Hold)**
   ```
   Key: hold:{showtimeId}:{seatId}
   Value: {userId}
   TTL: 120s
   ```

2. **Distributed Locks**
   ```
   Key: lock:{showtimeId}:{seatId}
   TTL: 30s
   Order: Always acquire in sorted order (prevent deadlock)
   ```

3. **Database Transaction**
   ```java
   @Transactional(isolation = Isolation.READ_COMMITTED)
   // Check seats not booked
   // Insert booking + booking_seats
   ```

4. **TOCTOU Prevention**
   ```
   1. Pre-check holds
   2. Acquire locks
   3. RE-CHECK holds (under lock)
   4. Create booking
   ```

---

## 💰 PRICING

| Seat Type | Calculation |
|-----------|-------------|
| STANDARD  | Base price × 1.0 |
| VIP       | Base price × 1.3 |

**Example:**
```
Base price (showtime): 150,000 VND
- Seat 1 (VIP): 150,000 × 1.3 = 195,000 VND
- Seat 2 (STANDARD): 150,000 × 1.0 = 150,000 VND
Total: 345,000 VND
```

---

## 🧪 TESTING

### **Test Cases:**

#### Happy Path
```bash
# 1. Hold seats
POST /api/v1/seats/hold
Expected: 200 OK

# 2. Create booking (within 120s)
POST /api/v1/bookings
Expected: 201 Created

# 3. Complete payment (within 15min)
POST /api/v1/bookings/{id}/payment
Expected: Redirect to VNPay
```

#### Error Cases
```bash
# 1. Book for past showtime
POST /api/v1/bookings (startTime < now)
Expected: 400 Bad Request

# 2. Hold expired
Wait > 120s → POST /api/v1/bookings
Expected: 409 Conflict - "Seat not held"

# 3. Concurrent booking
User A & B book same seats
Expected: One succeeds, one gets 409 Conflict

# 4. Payment timeout
Wait > 15 min without payment
Expected: Cron job expires booking
```

---

## 📊 DATABASE SCHEMA

### **Bookings Table:**

```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    booking_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,  -- ✅ Added for expiration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_booking_status (status),
    INDEX idx_booking_expires_at (expires_at),  -- ✅ For cron query
    INDEX idx_booking_account (account_id),
    INDEX idx_booking_showtime (showtime_id)
);
```

### **Booking Status Enum:**

```java
public enum BookingStatus {
    PENDING_PAYMENT,  // Just created, awaiting payment
    CONFIRMED,        // Payment successful
    CANCELLED,        // User cancelled or payment failed
    EXPIRED,          // 15min timeout, auto-expired by cron
    REFUNDED,         // Admin refund (future)
    USED              // User checked in (future)
}
```

---

## 🔧 CONFIGURATION

```yaml
# application.yml
booking:
  seat-hold-ttl-seconds: 120
  payment-timeout-minutes: 15
  cutoff-before-showtime-minutes: 15
  seat-lock-timeout-seconds: 30
  
pricing:
  vip-multiplier: 1.3
  standard-multiplier: 1.0
```

---

## 🐛 KNOWN ISSUES (FIXED)

| Issue | Status | Fix |
|-------|--------|-----|
| No showtime validation | ✅ Fixed | Added time checks |
| Seats locked forever | ✅ Fixed | Added expiration cron |
| Race conditions | ✅ Fixed | Distributed locks + TOCTOU prevention |
| Double booking | ✅ Fixed | Pessimistic DB locks |

---

## 📚 RELATED DOCS

- [Payment Flow](05-PAYMENT-FLOW.md) - Continue after booking created
- [API Documentation](02-API-DOCUMENTATION.md) - Full API reference
- [Testing Guide](07-TESTING-GUIDE.md) - Test strategies

---

## 🎯 NEXT STEPS

After booking created, user must complete payment:
→ See [Payment Flow Documentation](05-PAYMENT-FLOW.md)
