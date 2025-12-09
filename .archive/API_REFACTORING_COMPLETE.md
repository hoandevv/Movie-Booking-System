# API REFACTORING COMPLETE ✅

## 📋 Tổng quan thay đổi

Dự án đã được refactor hoàn toàn theo chuẩn REST API conventions với các cải tiến quan trọng:

### ✨ Những gì đã thay đổi

1. **✅ API Versioning**: Tất cả endpoints giờ có prefix `/api/v1`
2. **✅ Public/Private Separation**: Phân tách rõ ràng endpoints công khai và yêu cầu authentication
3. **✅ REST Conventions**: Tuân thủ nguyên tắc RESTful (HTTP verbs, resource naming)
4. **✅ Security Config**: Cấu hình bảo mật chính xác cho từng loại endpoint
5. **✅ Postman Collection**: Collection hoàn chỉnh với test scripts và environment

---

## 🔄 Migration Guide

### Thay đổi URLs (Old → New)

```
Authentication:
/api/auth/*          → /api/v1/auth/*

Movies:
/api/movies          → /api/v1/movies
/api/movies/search   → /api/v1/movies/search

Theaters:
/api/theaters        → /api/v1/theaters
/api/theaters/{id}/movies → /api/v1/theaters/{id}/movies

Showtimes:
/api/showtimes                      → /api/v1/showtimes
/api/showtimes/by-theater-and-movie → /api/v1/showtimes?theaterId=X&movieId=Y&date=Z
                                      (Thay đổi: dùng query params)

Screens & Seats:
/api/screens         → /api/v1/screens
/api/seats           → /api/v1/seats
/api/seats/generate/{screenId} → /api/v1/seats/generate/{screenId}

Seat Holds:
/api/seats/hold      → /api/v1/seat-holds      (POST)
/api/seats/release   → /api/v1/seat-holds      (DELETE - thay đổi method!)

Bookings:
/api/bookings        → /api/v1/bookings

Payments:
/api/payments/create/{id}         → /api/v1/payments/bookings/{id}/payment
/api/payments/vnpay/return        → /api/v1/payments/vnpay/callback
/api/payments/vnpay/verify        → /api/v1/payments/vnpay/verify
/api/payments/callback            → /api/v1/payments/webhook
/api/payments/verify/{id}         → /api/v1/payments/bookings/{id}/payment-status
/api/payments/cancel/{id}         → /api/v1/payments/bookings/{id}/payment (DELETE)

Vouchers:
/api/v1/vouchers/public           → /api/v1/vouchers (GET - public)
/api/v1/vouchers/my-usage         → /api/v1/vouchers/usages/me
/api/v1/vouchers (admin CRUD)     → /api/v1/vouchers/admin/*

OTP:
/api/otp/resend      → /api/v1/otp/send
```

---

## 🌍 Public vs Private Endpoints

### PUBLIC (Không cần authentication)

**Authentication:**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/activate`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/otp/send`

**Read-only resources:**
- `GET /api/v1/movies/**` - Xem phim, tìm kiếm
- `GET /api/v1/theaters/**` - Xem rạp
- `GET /api/v1/showtimes/**` - Xem lịch chiếu
- `GET /api/v1/screens/**` - Xem phòng chiếu
- `GET /api/v1/seats/**` - Xem sơ đồ ghế
- `GET /api/v1/vouchers` - Xem voucher công khai

**Webhooks:**
- `GET /api/v1/payments/vnpay/callback` - VNPay return URL
- `POST /api/v1/payments/webhook` - Payment webhook

### PRIVATE (Yêu cầu JWT token)

**User operations:**
- `GET /api/v1/auth/me` - Thông tin user
- `POST /api/v1/bookings` - Đặt vé
- `GET /api/v1/bookings` - Xem booking của mình
- `POST /api/v1/seat-holds` - Giữ ghế tạm thời
- `DELETE /api/v1/seat-holds` - Hủy giữ ghế
- `POST /api/v1/payments/**` - Thanh toán
- `POST /api/v1/vouchers/validate` - Validate voucher
- `GET /api/v1/vouchers/usages/me` - Lịch sử voucher

### ADMIN (Role ADMIN required)

**CRUD operations:**
- `POST/PATCH/DELETE /api/v1/movies`
- `POST/PATCH/DELETE /api/v1/theaters`
- `POST/PATCH/DELETE /api/v1/showtimes`
- `POST/PATCH/DELETE /api/v1/screens`
- `POST /api/v1/seats/generate/{screenId}`
- `GET/POST/PUT/DELETE /api/v1/vouchers/admin/**`

---

## 🧪 Testing với Postman

### 1. Import Collection & Environment

**Files cần import:**
- `Movie_Booking_System_V1_Collection.postman_collection.json`
- `Movie_Booking_System_Local.postman_environment.json`

**Cách import:**
1. Mở Postman
2. Click **Import** button
3. Chọn 2 files trên
4. Select environment: **Movie Booking System - Local Development**

### 2. Test Flow chuẩn

#### A. Authentication Flow

```
1. Register → POST /api/v1/auth/register
2. Send OTP → POST /api/v1/otp/send
3. Activate → POST /api/v1/auth/activate
4. Login → POST /api/v1/auth/login
   ✅ accessToken được tự động lưu vào environment
5. Get Current User → GET /api/v1/auth/me
```

#### B. Booking Flow (User đã login)

```
1. Browse Movies → GET /api/v1/movies
2. Search Movies → GET /api/v1/movies/search?keyword=avengers
3. Get Theaters → GET /api/v1/theaters
4. Get Showtimes → GET /api/v1/showtimes?theaterId=1&movieId=2&date=2025-11-11
5. View Seats → GET /api/v1/seats/screen/1
6. Hold Seats → POST /api/v1/seat-holds
7. Create Booking → POST /api/v1/bookings
   ✅ bookingId được tự động lưu
8. Create Payment → POST /api/v1/payments/bookings/{bookingId}/payment
   ✅ paymentUrl được trả về
9. (User thanh toán trên VNPay gateway)
10. Check Payment Status → GET /api/v1/payments/bookings/{bookingId}/payment-status
```

#### C. Admin Flow

```
1. Login as Admin
2. Create Movie → POST /api/v1/movies
3. Create Theater → POST /api/v1/theaters
4. Create Screen → POST /api/v1/screens
5. Generate Seats → POST /api/v1/seats/generate/{screenId}
6. Create Showtime → POST /api/v1/showtimes
7. Create Voucher → POST /api/v1/vouchers/admin
```

### 3. Environment Variables

Các biến tự động được set khi test:

| Variable | Description | Auto-set by |
|----------|-------------|-------------|
| `accessToken` | JWT access token | Login request |
| `refreshToken` | JWT refresh token | Login request |
| `bookingId` | Latest booking ID | Create Booking |
| `showtimeId` | Showtime ID for testing | Manual/Script |
| `seatIds` | Seat IDs for testing | Manual/Script |
| `paymentUrl` | VNPay payment URL | Create Payment |

---

## 🔒 Security Changes

### SecurityConfig.java

**Before:**
```java
// ❌ Tất cả endpoints yêu cầu authentication trừ auth endpoints
.anyRequest().authenticated()
```

**After:**
```java
// ✅ Phân tách rõ ràng
// Public endpoints
.requestMatchers(PUBLIC_ENDPOINTS).permitAll()

// Public GET only
.requestMatchers(HttpMethod.GET, "/api/v1/movies/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/theaters/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()

// Admin endpoints
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

// Authenticated endpoints
.requestMatchers("/api/v1/bookings/**").authenticated()
.requestMatchers("/api/v1/seat-holds/**").authenticated()

.anyRequest().authenticated()
```

---

## 📊 API Endpoints Summary

### Tổng số endpoints: **60+**

| Category | Public | Private | Admin | Total |
|----------|--------|---------|-------|-------|
| Authentication | 7 | 1 | 0 | 8 |
| Movies | 3 | 0 | 4 | 7 |
| Theaters | 3 | 0 | 3 | 6 |
| Showtimes | 2 | 0 | 3 | 5 |
| Screens | 2 | 0 | 3 | 5 |
| Seats | 3 | 0 | 3 | 6 |
| Seat Holds | 0 | 2 | 0 | 2 |
| Bookings | 0 | 4 | 0 | 4 |
| Payments | 2 | 4 | 0 | 6 |
| Vouchers | 1 | 2 | 5 | 8 |
| OTP | 1 | 0 | 0 | 1 |
| **TOTAL** | **24** | **13** | **21** | **58** |

---

## 🚀 Next Steps

### 1. Update Frontend (nếu có)

Đổi tất cả API calls sang URLs mới:

```javascript
// Before
const response = await fetch('/api/movies');

// After
const response = await fetch('/api/v1/movies');
```

### 2. Update VNPay Configuration

Trong môi trường production, update `application.yml`:

```yaml
vnpay:
  returnUrl: https://yourfrontend.com/payment/result
  ipnUrl: https://yourbackend.com/api/v1/payments/webhook
```

### 3. Test End-to-End

1. Chạy application: `mvn spring-boot:run`
2. Import Postman collection
3. Run từng folder tests trong Postman
4. Verify tất cả responses đúng

### 4. Documentation

API documentation tự động tại:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs/movie-booking-system`

---

## 📝 Breaking Changes

⚠️ **QUAN TRỌNG:** Các thay đổi này **KHÔNG TƯƠNG THÍCH NGƯỢC**

1. **URL Changes**: Tất cả endpoints có `/v1` prefix
2. **Path Changes**: 
   - `/showtimes/by-theater-and-movie` → query params
   - `/seats/hold` → `/seat-holds`
   - `/payments/create/{id}` → `/payments/bookings/{id}/payment`
3. **HTTP Method Changes**:
   - Release seats: `POST /seats/release` → `DELETE /seat-holds`
   - Cancel payment: `POST /payments/cancel/{id}` → `DELETE /payments/bookings/{id}/payment`

---

## 💡 Tips

### Sử dụng Postman Collection Runner

1. Select folder "Authentication"
2. Click "Run" button
3. Chọn environment
4. Run toàn bộ tests tự động

### Auto-save tokens

Tất cả JWT tokens được tự động lưu vào environment variables khi login thành công. Không cần copy-paste!

### Test Scripts

Mỗi request có pre-request và test scripts để:
- Tự động lưu IDs (bookingId, showtimeId)
- Verify response status
- Validate response structure

---

## 🐛 Troubleshooting

### 401 Unauthorized

→ Token hết hạn. Gọi lại `/api/v1/auth/login` hoặc `/api/v1/auth/refresh-token`

### 403 Forbidden

→ Không đủ quyền. Endpoint yêu cầu ADMIN role.

### 404 Not Found

→ Kiểm tra lại URL. Đảm bảo có `/v1` prefix.

### VNPay webhook không hoạt động

→ Trong môi trường local, VNPay không thể gọi webhook. Sử dụng `/vnpay/callback` để test.

---

## 📚 References

- [API Restructure Plan](./API_RESTRUCTURE_PLAN.md) - Chi tiết đầy đủ về thay đổi
- [Postman Collection](./Movie_Booking_System_V1_Collection.postman_collection.json)
- [Postman Environment](./Movie_Booking_System_Local.postman_environment.json)

---

**🎉 DONE! API đã được refactor hoàn chỉnh theo chuẩn REST conventions!**

Nếu có câu hỏi, kiểm tra file `API_RESTRUCTURE_PLAN.md` để xem bảng so sánh chi tiết.
