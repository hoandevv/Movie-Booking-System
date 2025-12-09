# BOOKING SYSTEM - DOCUMENTATION

## 📋 TỔNG QUAN FLOW BOOKING

Hệ thống booking ghế xem phim với 3 bước chính:
1. **Hold Seats** - User giữ ghế tạm thời (120s)
2. **Create Booking** - Tạo booking với status PENDING_PAYMENT
3. **Payment** - Thanh toán và confirm booking

---

## 🔄 FLOW HOÀN CHỈNH

### **BƯỚC 1: User Hold Seats**

**Endpoint:** `POST /api/seats/hold`

**Request:**
```json
{
  "showtimeId": 123,
  "seatIds": [1, 2, 3],
  "ttlSec": 120
}
```

**Process:**
1. User chọn ghế trên UI
2. Call API hold seats
3. Redis lưu hold với TTL = 120s
4. Sử dụng **ATOMIC SETNX** để tránh race condition
5. Nếu ghế đã bị hold bởi user khác → Conflict error

**Response:**
- ✅ Success: Ghế được hold trong 120s
- ❌ Fail: Ghế đang được hold bởi user khác

---

### **BƯỚC 2: User Create Booking**

**Endpoint:** `POST /api/bookings`

**Request:**
```json
{
  "showtimeId": 123,
  "seatIds": [1, 2, 3]
}
```

**Process (QUAN TRỌNG - ĐÃ FIX):**

1. **Validate showtime tồn tại**
2. **Verify seats đang được hold bởi current user** (pre-check)
3. **Lock từng ghế riêng lẻ** (sorted order để tránh deadlock)
   ```
   Lock: seat:lock:123:1
   Lock: seat:lock:123:2
   Lock: seat:lock:123:3
   ```
4. **Re-verify holds dưới lock** (tránh TOCTOU attack)
5. **Check DB**: Ghế đã booked chưa (PENDING_PAYMENT hoặc CONFIRMED)
6. **Tạo Booking entity** với status = PENDING_PAYMENT
7. **Tính giá**:
   - VIP seat: price × 1.3
   - Standard seat: price × 1.0
8. **Save vào DB**:
   - `bookings` table
   - `booking_seats` table
9. **🔥 CRITICAL: Consume Redis hold** (xóa hold vì đã persist vào DB)
10. **Release locks**

**Response:**
```json
{
  "success": true,
  "result": {
    "id": 456,
    "status": "PENDING_PAYMENT",
    "totalPrice": 250000,
    "bookingDate": "2025-11-07T10:30:00",
    "seats": [...]
  },
  "message": "Booking created. Please complete payment within 15 minutes."
}
```

**⏰ Timeout:** 15 phút. Sau đó booking sẽ EXPIRED tự động.

---

### **BƯỚC 3: User Payment**

#### **3.1. Tạo Payment URL**

**Endpoint:** `POST /api/payments/create/{bookingId}`

**Process:**
```
TODO: Integration với Payment Gateway (VNPay, MoMo, Stripe)
1. Generate payment request
2. Sign với secret key
3. Return payment URL
4. Frontend redirect user sang gateway
```

**Response:**
```json
{
  "success": true,
  "result": "https://payment-gateway.com/checkout?bookingId=456",
  "message": "Redirect to payment gateway"
}
```

#### **3.2. Payment Gateway Callback**

**Endpoint:** `POST /api/payments/callback`

**Request từ Gateway:**
```json
{
  "bookingId": 456,
  "transactionId": "TXN123456",
  "status": "SUCCESS",
  "amount": "250000",
  "signature": "abc123..."
}
```

**Process:**

**✅ Payment SUCCESS:**
1. Verify signature (TODO)
2. Update booking.status = **CONFIRMED**
3. Consume Redis hold (nếu còn)
4. Ghế đã CONFIRMED - không thể cancel

**❌ Payment FAILED:**
1. Update booking.status = **CANCELLED**
2. Release Redis hold
3. Ghế available cho user khác

---

## 🔒 BẢO MẬT & XỬ LÝ RACE CONDITION

### **1. Redis Hold với SETNX (Atomic)**
```java
// ❌ SAI - TOCTOU vulnerability
String owner = redis.get(key);
if (owner == null) {
    redis.set(key, userId); // ← Race condition ở đây
}

// ✅ ĐÚNG - Atomic operation
Boolean success = redis.setIfAbsent(key, userId, ttl);
if (!success) {
    // Check nếu chính user này đang hold (idempotent)
}
```

### **2. Distributed Lock theo từng ghế**
```java
// ❌ SAI - Lock theo group → deadlock
lockKey = "seatLock:123:1,2,3"

// ✅ ĐÚNG - Lock từng ghế, sorted order
for (seatId in sortedSeatIds) {
    lock("seat:lock:123:" + seatId)
}
```

### **3. Double Check Under Lock**
```java
// Pre-check (ngoài lock)
assertHeldByUser(seatIds, userId);

lock.acquire();
try {
    // Re-check (dưới lock) - tránh TOCTOU
    assertHeldByUser(seatIds, userId);
    
    // Check DB
    checkBookedSeatsInDB(seatIds);
    
    // Create booking
} finally {
    lock.release();
}
```

---

## 🗄️ DATABASE CONSTRAINTS

### **1. Ghế chỉ được book 1 lần cho mỗi showtime**

Query kiểm tra:
```sql
SELECT bs.seat_id
FROM booking_seats bs
JOIN bookings b ON bs.booking_id = b.id
WHERE b.showtime_id = :showtimeId
  AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED')
  AND bs.seat_id IN (:seatIds)
```

Nếu có kết quả → Ghế đã booked → Throw ConflictException

### **2. Booking States**

```
PENDING_PAYMENT → (payment success) → CONFIRMED
PENDING_PAYMENT → (payment failed) → CANCELLED
PENDING_PAYMENT → (timeout 15min) → EXPIRED
```

**QUAN TRỌNG:** 
- EXPIRED bookings **KHÔNG xóa** booking_seats (để audit)
- Chỉ update status
- Redis hold được release tự động (TTL) hoặc bởi cron job

---

## ⏱️ TIMEOUT & CRON JOB

### **Cron Job: Expire Bookings**

**Schedule:** Mỗi 5 phút
```
0 */5 * * * *
```

**Process:**
```java
1. Find bookings với status = PENDING_PAYMENT 
   AND bookingDate < now - 15 minutes

2. For each booking:
   - Update status = EXPIRED
   - Release Redis hold (nếu còn)
   - KHÔNG xóa booking_seats (giữ để audit)

3. Log số lượng booking đã expire
```

---

## 🚨 ERROR HANDLING

### **Conflict Errors**

| Error | Cause | Solution |
|-------|-------|----------|
| Ghế đang được giữ bởi người khác | Redis hold exists | Chọn ghế khác hoặc đợi TTL hết |
| Ghế đã được booking | DB có record | Chọn ghế khác |
| Hold đã hết hạn | Redis TTL expired | Hold lại ghế |
| Lock timeout | Không acquire được lock | Retry |

### **Validation Errors**

| Error | Cause |
|-------|-------|
| Showtime not found | Invalid showtimeId |
| Seat not found | Invalid seatId |
| Booking not found | Invalid bookingId |
| Invalid status transition | Confirm đã EXPIRED booking |

---

## 📊 REDIS DATA STRUCTURE

```
Key Pattern: hold:{showtimeId}:{seatId}
Value: {userId}
TTL: 120 seconds (default)

Example:
hold:123:1 = "789" (TTL: 118s)
hold:123:2 = "789" (TTL: 118s)
hold:123:3 = "456" (TTL: 95s) ← Người khác hold
```

---

## 🎯 API SUMMARY

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/seats/hold` | Hold ghế tạm thời |
| POST | `/api/seats/release` | Release hold thủ công |
| POST | `/api/bookings` | Tạo booking (PENDING_PAYMENT) |
| POST | `/api/payments/create/{bookingId}` | Tạo payment URL |
| POST | `/api/payments/callback` | Payment gateway callback |
| GET | `/api/payments/verify/{bookingId}` | Check payment status |
| POST | `/api/payments/cancel/{bookingId}` | Cancel payment |

---

## 🔐 AUTHENTICATION

Tất cả endpoints yêu cầu JWT token (trừ payment callback từ gateway)

```http
Authorization: Bearer {accessToken}
```

Xem thêm: `JWT_AUTHENTICATION_GUIDE.md`

---

## ✅ ĐÃ FIX

1. ✅ **Race condition** - Dùng Redis SETNX atomic
2. ✅ **Lock strategy** - Lock từng ghế thay vì group
3. ✅ **TOCTOU** - Double check under lock
4. ✅ **Memory leak** - Consume hold sau khi booking
5. ✅ **Audit trail** - Không xóa booking_seats khi expire
6. ✅ **Distributed lock** - UUID owner để tránh unlock nhầm

## 🚧 TODO

1. 🚧 **Payment Gateway Integration** (VNPay/MoMo/Stripe)
2. 🚧 **Signature verification** cho payment callback
3. 🚧 **Webhook notification** cho user khi payment success/fail
4. 🚧 **Email confirmation** sau khi booking confirmed
5. 🚧 **QR Code** cho vé đã confirmed

---

**Last Updated:** 2025-11-07
**Version:** 2.0 (Fixed Race Conditions & Concurrency Issues)

