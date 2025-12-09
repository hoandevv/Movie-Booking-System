# API RESTRUCTURE PLAN - Movie Booking System

## 📋 TÓM TẮT THAY ĐỔI

### Mục tiêu
1. ✅ Thêm API versioning (`/api/v1`)
2. ✅ Phân tách rõ ràng Public vs Private endpoints
3. ✅ Chuẩn hóa REST API conventions
4. ✅ Fix SecurityConfig cho đúng logic
5. ✅ Tạo Postman Collection đầy đủ

### Nguyên tắc phân loại endpoints

#### 🌍 PUBLIC ENDPOINTS (Không cần authentication)
- Xem danh sách phim, tìm kiếm phim, chi tiết phim
- Xem danh sách rạp, chi tiết rạp
- Xem lịch chiếu theo rạp/phim
- Xem sơ đồ ghế của một suất chiếu
- Authentication endpoints (login, register, forgot password)

#### 🔒 PRIVATE ENDPOINTS (Yêu cầu authentication)
- Tất cả Booking operations
- Tất cả Payment operations
- Hold/Release seats
- Voucher validation & usage history
- User profile management

#### 👑 ADMIN ENDPOINTS (Role ADMIN/MANAGER)
- CRUD Movies, Theaters, Screens, Showtimes
- Generate seats
- Voucher management (CRUD)
- System statistics

---

## 📊 BẢNG SO SÁNH CHI TIẾT

### 1. Authentication & Authorization

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `/api/auth/register` | `/api/v1/auth/register` | Public | POST | Đăng ký tài khoản |
| `/api/auth/login` | `/api/v1/auth/login` | Public | POST | Đăng nhập |
| `/api/auth/logout` | `/api/v1/auth/logout` | Public | POST | Đăng xuất |
| `/api/auth/activate` | `/api/v1/auth/activate` | Public | POST | Kích hoạt tài khoản |
| `/api/auth/refresh-token` | `/api/v1/auth/refresh-token` | Public | POST | Refresh JWT token |
| `/api/auth/forgot-password` | `/api/v1/auth/forgot-password` | Public | POST | Quên mật khẩu |
| `/api/auth/reset-password` | `/api/v1/auth/reset-password` | Public | POST | Reset mật khẩu |
| `/api/auth/me` | `/api/v1/auth/me` | Private | GET | Lấy thông tin user hiện tại |

### 2. Movies

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/movies` | `POST /api/v1/movies` | Admin | POST | Tạo phim mới |
| `PATCH /api/movies/{id}` | `PATCH /api/v1/movies/{id}` | Admin | PATCH | Cập nhật phim |
| `DELETE /api/movies/{id}` | `DELETE /api/v1/movies/{id}` | Admin | DELETE | Xóa phim (soft delete) |
| `GET /api/movies` | `GET /api/v1/movies` | **Public** | GET | Danh sách phim (pagination) |
| `GET /api/movies/{id}` | `GET /api/v1/movies/{id}` | **Public** | GET | Chi tiết phim |
| `GET /api/movies/search` | `GET /api/v1/movies/search` | **Public** | GET | Tìm kiếm phim |
| `GET /api/movies/count` | `GET /api/v1/movies/count` | Admin | GET | Đếm tổng số phim |

### 3. Theaters

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/theaters` | `POST /api/v1/theaters` | Admin | POST | Tạo rạp mới |
| `PATCH /api/theaters/{id}` | `PATCH /api/v1/theaters/{id}` | Admin | PATCH | Cập nhật rạp |
| `DELETE /api/theaters/{id}` | `DELETE /api/v1/theaters/{id}` | Admin | DELETE | Xóa rạp |
| `GET /api/theaters` | `GET /api/v1/theaters` | **Public** | GET | Danh sách rạp |
| `GET /api/theaters/{id}` | `GET /api/v1/theaters/{id}` | **Public** | GET | Chi tiết rạp |
| `GET /api/theaters/{id}/movies` | `GET /api/v1/theaters/{id}/movies` | **Public** | GET | Phim đang chiếu tại rạp |
| `GET /api/theaters/count` | `GET /api/v1/theaters/count` | Admin | GET | Đếm tổng số rạp |

### 4. Screens (Phòng chiếu)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/screens` | `POST /api/v1/screens` | Admin | POST | Tạo phòng chiếu |
| `PATCH /api/screens/{id}` | `PATCH /api/v1/screens/{id}` | Admin | PATCH | Cập nhật phòng |
| `DELETE /api/screens/{id}` | `DELETE /api/v1/screens/{id}` | Admin | DELETE | Xóa phòng |
| `GET /api/screens` | `GET /api/v1/screens` | Admin | GET | Danh sách phòng |
| `GET /api/screens/{id}` | `GET /api/v1/screens/{id}` | **Public** | GET | Chi tiết phòng |
| `GET /api/screens/theater/{id}` | `GET /api/v1/theaters/{id}/screens` | **Public** | GET | Phòng chiếu theo rạp |
| `GET /api/screens/count` | `GET /api/v1/screens/count` | Admin | GET | Đếm số phòng |

**Thay đổi lớn:** Nested resource `/theaters/{id}/screens` thay vì `/screens/theater/{id}`

### 5. Showtimes (Lịch chiếu)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/showtimes` | `POST /api/v1/showtimes` | Admin | POST | Tạo lịch chiếu |
| `PATCH /api/showtimes/{id}` | `PATCH /api/v1/showtimes/{id}` | Admin | PATCH | Cập nhật lịch chiếu |
| `DELETE /api/showtimes/{id}` | `DELETE /api/v1/showtimes/{id}` | Admin | DELETE | Xóa lịch chiếu |
| `GET /api/showtimes` | `GET /api/v1/showtimes` | **Public** | GET | Danh sách lịch chiếu |
| `GET /api/showtimes/{id}` | `GET /api/v1/showtimes/{id}` | **Public** | GET | Chi tiết lịch chiếu |
| `GET /api/showtimes/by-theater-and-movie?theaterId=X&movieId=Y&date=Z` | `GET /api/v1/showtimes?theaterId=X&movieId=Y&date=Z` | **Public** | GET | Lọc theo rạp/phim/ngày |
| `GET /api/showtimes/count` | `GET /api/v1/showtimes/count` | Admin | GET | Đếm số suất chiếu |

**Thay đổi lớn:** Bỏ `/by-theater-and-movie`, dùng query params trực tiếp

### 6. Seats (Ghế ngồi)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/seats` | `POST /api/v1/seats` | Admin | POST | Tạo ghế thủ công |
| `POST /api/seats/generate/{screenId}` | `POST /api/v1/screens/{screenId}/seats/generate` | Admin | POST | Generate ghế tự động |
| `PATCH /api/seats/{id}` | `PATCH /api/v1/seats/{id}` | Admin | PATCH | Cập nhật ghế |
| `DELETE /api/seats/{id}` | `DELETE /api/v1/seats/{id}` | Admin | DELETE | Xóa ghế |
| `GET /api/seats` | `GET /api/v1/seats` | Admin | GET | Danh sách ghế |
| `GET /api/seats/{id}` | `GET /api/v1/seats/{id}` | **Public** | GET | Chi tiết ghế |
| `GET /api/seats/screen/{id}` | `GET /api/v1/screens/{id}/seats` | **Public** | GET | Ghế theo phòng chiếu |
| `GET /api/seats/screen/{id}/status/{status}` | `GET /api/v1/screens/{id}/seats?status={status}` | **Public** | GET | Ghế theo trạng thái |

**Thay đổi lớn:** Nested resources + query params

### 7. Seat Holds (Giữ ghế tạm thời)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/seats/hold` | `POST /api/v1/seat-holds` | Private | POST | Giữ ghế (120s) |
| `POST /api/seats/release` | `DELETE /api/v1/seat-holds` | Private | DELETE | Hủy giữ ghế |

**Thay đổi lớn:** 
- Tách riêng thành `/seat-holds` resource
- Dùng DELETE thay vì POST cho release
- **Lưu ý:** Có thể refactor sâu hơn thành `/api/v1/showtimes/{id}/seat-holds` nhưng phức tạp hơn

### 8. Bookings (Đặt vé)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/bookings` | `POST /api/v1/bookings` | Private | POST | Tạo booking |
| `PATCH /api/bookings/{id}` | `PATCH /api/v1/bookings/{id}` | Private | PATCH | Cập nhật booking |
| `DELETE /api/bookings/{id}` | `DELETE /api/v1/bookings/{id}` | Private | DELETE | Hủy booking |
| `GET /api/bookings` | `GET /api/v1/bookings` | Private | GET | Danh sách booking |
| `GET /api/bookings/{id}` | `GET /api/v1/bookings/{id}` | Private | GET | Chi tiết booking |

### 9. Payments (Thanh toán)

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/payments/create/{bookingId}` | `POST /api/v1/bookings/{bookingId}/payments` | Private | POST | Tạo payment URL |
| `GET /api/payments/vnpay/return` | `GET /api/v1/payments/vnpay/callback` | Public | GET | VNPay return callback |
| `POST /api/payments/vnpay/verify` | `POST /api/v1/payments/vnpay/verify` | Private | POST | Verify VNPay payment |
| `POST /api/payments/callback` | `POST /api/v1/payments/webhook` | Public | POST | Generic payment webhook |
| `GET /api/payments/verify/{bookingId}` | `GET /api/v1/bookings/{bookingId}/payment-status` | Private | GET | Check payment status |
| `POST /api/payments/cancel/{bookingId}` | `DELETE /api/v1/bookings/{bookingId}/payment` | Private | DELETE | Cancel payment |

**Thay đổi lớn:**
- Payment là sub-resource của Booking
- Dùng HTTP verbs chuẩn (DELETE thay POST)
- Webhook endpoints rõ ràng hơn

### 10. Vouchers

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/v1/vouchers/validate` | `POST /api/v1/vouchers/validate` | Private | POST | Validate voucher |
| `GET /api/v1/vouchers/public` | `GET /api/v1/vouchers` | **Public** | GET | Danh sách voucher công khai |
| `GET /api/v1/vouchers/my-usage` | `GET /api/v1/voucher-usages/me` | Private | GET | Lịch sử dùng voucher |
| `POST /api/v1/vouchers` | `POST /api/v1/admin/vouchers` | Admin | POST | Tạo voucher |
| `PUT /api/v1/vouchers/{id}` | `PUT /api/v1/admin/vouchers/{id}` | Admin | PUT | Cập nhật voucher |
| `DELETE /api/v1/vouchers/{id}` | `DELETE /api/v1/admin/vouchers/{id}` | Admin | DELETE | Xóa voucher |
| `GET /api/v1/vouchers/{id}` | `GET /api/v1/admin/vouchers/{id}` | Admin | GET | Chi tiết voucher |
| `GET /api/v1/vouchers` (admin) | `GET /api/v1/admin/vouchers` | Admin | GET | Tất cả vouchers |

**Thay đổi lớn:**
- Tách admin endpoints vào `/admin/vouchers`
- Public GET `/vouchers` chỉ trả public vouchers
- Voucher usage tách riêng resource

### 11. OTP

| Cũ | Mới | Access Level | Method | Mô tả |
|---|---|---|---|---|
| `POST /api/otp/resend` | `POST /api/v1/otp/send` | Public | POST | Gửi OTP |

---

## 🔐 SECURITY CONFIG CHANGES

### Before
```java
public static final String[] PUBLIC_AUTH_ENDPOINTS = {
    "/",
    "/api/auth/register",
    "/api/auth/login",
    // ... only auth endpoints
};

.authorizeHttpRequests(authz -> authz
    .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
    .anyRequest().authenticated() // ❌ Block tất cả
)
```

### After
```java
public static final String[] PUBLIC_ENDPOINTS = {
    "/",
    // Auth
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/logout",
    "/api/v1/auth/activate",
    "/api/v1/auth/refresh-token",
    "/api/v1/auth/forgot-password",
    "/api/v1/auth/reset-password",
    "/api/v1/otp/**",
    
    // Public read-only endpoints
    "/api/v1/movies/**",           // GET only
    "/api/v1/theaters/**",         // GET only
    "/api/v1/showtimes/**",        // GET only
    "/api/v1/screens/*/seats",     // GET seats for a screen
    "/api/v1/vouchers",            // GET public vouchers
    
    // Payment webhooks (no auth needed, verify by signature)
    "/api/v1/payments/vnpay/callback",
    "/api/v1/payments/webhook",
    
    // Swagger
    "/swagger-ui/**",
    "/v3/api-docs/**"
};

.authorizeHttpRequests(authz -> authz
    // Public endpoints
    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
    
    // Public GET only for resources
    .requestMatchers(HttpMethod.GET, "/api/v1/movies/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/theaters/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/screens/*/seats").permitAll()
    
    // Admin endpoints
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    
    // Authenticated endpoints
    .requestMatchers("/api/v1/bookings/**").authenticated()
    .requestMatchers("/api/v1/seat-holds/**").authenticated()
    .requestMatchers("/api/v1/voucher-usages/**").authenticated()
    
    // Default: authenticated
    .anyRequest().authenticated()
)
```

---

## 📝 RESPONSE FORMAT STANDARDIZATION

Tất cả APIs đều dùng `BaseResponse<T>`:

```java
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2025-11-11T10:30:00"
}
```

Error response:
```java
{
  "success": false,
  "message": "Error message",
  "errors": ["Detail 1", "Detail 2"],
  "timestamp": "2025-11-11T10:30:00"
}
```

---

## 🚀 MIGRATION CHECKLIST

- [ ] Update SecurityConfig với public endpoints
- [ ] Refactor tất cả Controllers với `/v1` prefix
- [ ] Update application.yml (VNPay URLs)
- [ ] Generate Postman Collection
- [ ] Update Frontend API calls (nếu có)
- [ ] Test toàn bộ APIs
- [ ] Update API documentation

---

## 🎯 BENEFITS SAU KHI REFACTOR

1. ✅ **Versioning**: Dễ dàng thêm /v2 mà không breaking changes
2. ✅ **Security**: Phân tách rõ ràng public/private/admin
3. ✅ **RESTful**: Tuân thủ REST conventions
4. ✅ **Developer Experience**: Dễ hiểu, dễ maintain
5. ✅ **Testability**: Postman collection đầy đủ
6. ✅ **Scalability**: Dễ thêm features mới

---

**Người thực hiện:** GitHub Copilot  
**Ngày:** 2025-11-11  
**Status:** ✅ Ready for implementation
