# 🎬 Movie Booking System - Backend API

> Hệ thống đặt vé xem phim trực tuyến với Spring Boot, Redis, và MySQL

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [📚 Tài liệu chính](#-tài-liệu-chính)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Tech Stack](#-tech-stack)
- [Cài đặt và chạy](#-cài-đặt-và-chạy)
- [Các tính năng chính](#-các-tính-năng-chính)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Flow nghiệp vụ](#-flow-nghiệp-vụ)
- [Security](#-security)
- [Testing](#-testing)

---

## 📚 Tài liệu chính

> **✨ NEW:** Tất cả tài liệu đã được tổ chức lại trong thư mục `/docs/`

| # | Document | Description |
|---|----------|-------------|
| 1 | [Setup Guide](docs/01-SETUP-GUIDE.md) | 🚀 Cài đặt & chạy dự án |
| 2 | [API Documentation](docs/02-API-DOCUMENTATION.md) | 📡 REST API endpoints |
| 3 | [Authentication](docs/03-AUTHENTICATION.md) | 🔐 JWT authentication guide |
| 4 | [Booking Flow](docs/04-BOOKING-FLOW.md) | 🎫 **Booking flow (FIXED)** |
| 5 | [Payment Flow](docs/05-PAYMENT-FLOW.md) | 💳 **Payment flow (FIXED)** |
| 6 | [TODO & Improvements](docs/06-TODO.md) | ✅ Pending tasks |
| 7 | [Testing Guide](docs/07-TESTING-GUIDE.md) | 🧪 Testing strategies |

**📖 Start here:** [Documentation Index](docs/README.md)

---

## 🎯 Tổng quan

**Movie Booking System** là một REST API backend cho phép người dùng:
- Đăng ký tài khoản, đăng nhập với JWT authentication
- Xem danh sách phim, rạp, suất chiếu
- Giữ chỗ (hold) ghế ngồi trong thời gian ngắn
- Đặt vé xem phim và thanh toán
- Quản lý phim, rạp, suất chiếu (dành cho Admin)

### 🎯 Mục tiêu dự án
- Xây dựng API RESTful chuẩn với Spring Boot
- Xử lý concurrent booking với Redis distributed lock
- Tối ưu performance với caching và indexing
- Bảo mật với JWT + Spring Security
- Thiết kế database normalized, có index hợp lý

---

## 🏗️ Kiến trúc hệ thống

### Architecture Pattern
```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (Frontend)                       │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Controllers  │  │   Services   │  │ Repositories │      │
│  │  (REST API)  │─▶│ (Business)   │─▶│   (JPA)     │      │
│  └──────────────┘  └──────────────┘  └──────┬───────┘      │
│         │                 │                   │              │
│         │                 ▼                   │              │
│         │       ┌──────────────────┐         │              │
│         │       │  Redis (Cache +  │         │              │
│         │       │  Distributed Lock│         │              │
│         │       └──────────────────┘         │              │
│         │                                     │              │
│         ▼                                     ▼              │
│  ┌──────────────┐                  ┌──────────────┐        │
│  │ Security     │                  │    MySQL     │        │
│  │ (JWT Filter) │                  │  (Database)  │        │
│  └──────────────┘                  └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Layer Architecture
```
┌───────────────────────────────────────────────┐
│          Controller Layer                      │  ← REST endpoints
├───────────────────────────────────────────────┤
│          Service Layer                         │  ← Business logic
├───────────────────────────────────────────────┤
│          Repository Layer                      │  ← Data access (JPA)
├───────────────────────────────────────────────┤
│          Entity Layer                          │  ← Domain models
└───────────────────────────────────────────────┘
       ↕                ↕              ↕
┌──────────┐   ┌──────────────┐   ┌─────────┐
│  MySQL   │   │  Redis Cache │   │  Email  │
└──────────┘   └──────────────┘   └─────────┘
```

---

## 🛠️ Tech Stack

### Core Framework
- **Java 17** - Programming language
- **Spring Boot 3.3.4** - Application framework
- **Spring Data JPA** - ORM & database access
- **Spring Security** - Authentication & authorization
- **Spring Validation** - Input validation

### Database
- **MySQL 8** - Primary database
- **Redis** - Caching, distributed locks, seat holding
- **Hibernate** - JPA implementation

### Security & Authentication
- **JWT (jjwt 0.11.5)** - Token-based authentication
- **BCrypt** - Password hashing
- **Spring Security** - Security framework

### Additional Libraries
- **Lombok** - Boilerplate code reduction
- **MapStruct 1.5.5** - Object mapping
- **SpringDoc OpenAPI 2.5.0** - API documentation (Swagger)
- **JavaMail** - Email sending
- **Thymeleaf** - Email templates

### DevOps & Tools
- **Maven** - Build tool
- **Docker Compose** - Container orchestration
- **Testcontainers** - Integration testing
- **JaCoCo** - Code coverage

---

## 🚀 Cài đặt và chạy

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Docker & Docker Compose (optional)

### 1. Clone repository
```bash
git clone https://github.com/hoangdinhdung05/Backend-Movie-Booking-System.git
cd Backend-Movie-Booking-System
```

### 2. Cấu hình Database

**Tạo database MySQL:**
```sql
CREATE DATABASE movie_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Chỉnh sửa `src/main/resources/application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_booking
    username: root
    password: your_password
```

### 3. Cấu hình Redis
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. Cấu hình Email (Gmail SMTP)
```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-app-password  # App password, không phải password thường
```

### 5. Build và chạy

**Sử dụng Maven:**
```bash
mvn clean install
mvn spring-boot:run
```

**Hoặc sử dụng Docker Compose:**
```bash
docker-compose up -d
```

### 6. Truy cập application
- **API Base URL**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs/movie-booking-system

---

## ✨ Các tính năng chính

### 1. 👤 Authentication & User Management
- ✅ Đăng ký tài khoản với email verification (OTP)
- ✅ Login/Logout với JWT tokens (Access + Refresh)
- ✅ Refresh token khi access token hết hạn
- ✅ Forgot password & Reset password với OTP
- ✅ Rate limiting cho OTP (5 lần/ngày, 60s cooldown)

### 2. 🎬 Movie Management
- ✅ CRUD movies (Admin only)
- ✅ Search movies với filters (genre, language, rating, release date)
- ✅ Pagination & sorting
- ✅ Movie status: COMING_SOON, NOW_SHOWING, ENDED

### 3. 🏢 Theater & Screen Management
- ✅ CRUD theaters (Admin only)
- ✅ CRUD screens (Admin only)
- ✅ Auto-generate seats cho screen

### 4. 🎟️ Showtime Management
- ✅ CRUD showtimes (Admin only)
- ✅ Unique constraint: screen + movie + time
- ✅ Get showtimes by theater & movie
- ✅ Dynamic pricing per showtime

### 5. 💺 Seat Management
- ✅ Auto-generate seats (STANDARD/VIP)
- ✅ **Distributed seat holding với Redis** (TTL: 120s)
- ✅ Redis distributed locks để tránh race condition
- ✅ VIP seats có giá cao hơn 30%

### 6. 🎫 Booking Flow
- ✅ Hold seats trước khi booking
- ✅ Create booking với validation
- ✅ Atomic seat locking (SETNX)
- ✅ Auto-calculate total price (base + VIP markup)
- ✅ Booking status: PENDING_PAYMENT → CONFIRMED/EXPIRED

### 7. 💳 Payment Flow (⚠️ MOCK - Cần integration)
- ⚠️ Create payment URL (hiện tại là mock)
- ⚠️ Payment callback handler (chưa verify signature)
- ⚠️ Payment verification
- ⚠️ Cancel payment

### 8. ⏰ Scheduled Tasks
- ✅ Auto-expire bookings sau 15 phút (chạy mỗi 5 phút)
- ✅ Release seats khi booking expired

### 9. 🔐 Security
- ✅ JWT-based authentication
- ✅ Role-based access control (USER, ADMIN, THEATER_MANAGEMENT)
- ✅ Password encryption với BCrypt
- ✅ CORS configuration
- ✅ Stateless session management

---

## 📚 API Documentation

### Authentication Endpoints
```
POST   /api/auth/register          - Đăng ký tài khoản
POST   /api/auth/activate           - Kích hoạt tài khoản (OTP)
POST   /api/auth/login              - Đăng nhập
POST   /api/auth/logout             - Đăng xuất
POST   /api/auth/refresh-token      - Refresh access token
POST   /api/auth/forgot-password    - Quên mật khẩu
POST   /api/auth/reset-password     - Đặt lại mật khẩu
GET    /api/auth/me                 - Test authentication
```

### Movie Endpoints
```
GET    /api/movies                  - Lấy danh sách phim (pagination)
GET    /api/movies/{id}             - Lấy chi tiết phim
GET    /api/movies/search           - Tìm kiếm phim với filters
POST   /api/movies                  - Tạo phim mới (ADMIN)
PATCH  /api/movies/{id}             - Cập nhật phim (ADMIN)
DELETE /api/movies/{id}             - Xóa phim (ADMIN)
```

### Showtime Endpoints
```
GET    /api/showtimes                        - Lấy danh sách suất chiếu
GET    /api/showtimes/{id}                   - Chi tiết suất chiếu
GET    /api/showtimes/by-theater-and-movie   - Lấy suất chiếu theo rạp & phim
POST   /api/showtimes                        - Tạo suất chiếu (ADMIN)
PATCH  /api/showtimes/{id}                   - Cập nhật (ADMIN)
DELETE /api/showtimes/{id}                   - Xóa (ADMIN)
```

### Seat & Booking Endpoints
```
POST   /api/seats/hold              - Hold ghế tạm thời (120s)
POST   /api/seats/release           - Release ghế đã hold
POST   /api/bookings                - Tạo booking (sau khi hold)
GET    /api/bookings/{id}           - Chi tiết booking
```

### Payment Endpoints (⚠️ Mock)
```
POST   /api/payments/create/{bookingId}   - Tạo payment URL
POST   /api/payments/callback             - Payment gateway callback
GET    /api/payments/verify/{bookingId}   - Verify payment status
POST   /api/payments/cancel/{bookingId}   - Cancel payment
```

**Chi tiết đầy đủ**: Xem Swagger UI tại `http://localhost:8080/swagger-ui.html`

---

## 🗄️ Database Schema

### Core Tables

#### 1. **accounts** - Tài khoản người dùng
```sql
- id (PK)
- username (UNIQUE)
- email (UNIQUE)
- password (BCrypt hashed)
- email_verified (boolean)
- status (ACTIVE/INACTIVE/BANNED)
- created_at, updated_at
```

#### 2. **user** - Thông tin cá nhân
```sql
- id (PK)
- account_id (FK → accounts, UNIQUE)
- first_name, last_name
- phone_number
```

#### 3. **roles** - Vai trò
```sql
- id (PK)
- name (USER/ADMIN/THEATER_MANAGEMENT)
```

#### 4. **account_has_role** - Bảng trung gian
```sql
- account_id (FK)
- role_id (FK)
- Primary Key: (account_id, role_id)
```

#### 5. **movies** - Phim
```sql
- id (PK)
- title, description
- duration (phút)
- release_date
- poster_url, trailer_url
- rating (0-10)
- genre, language
- status (COMING_SOON/NOW_SHOWING/ENDED)
- created_at, updated_at
```

#### 6. **theaters** - Rạp chiếu phim
```sql
- id (PK)
- name, location
- status (ACTIVE/INACTIVE)
```

#### 7. **screens** - Phòng chiếu
```sql
- id (PK)
- theater_id (FK → theaters)
- name (e.g., "Screen 1")
- total_seats
- status (ACTIVE/INACTIVE/MAINTENANCE)
```

#### 8. **seats** - Ghế ngồi
```sql
- id (PK)
- screen_id (FK → screens)
- row_label (A, B, C...)
- seat_number (1, 2, 3...)
- seat_type (STANDARD/VIP)
- status (AVAILABLE/UNAVAILABLE)
- is_deleted (soft delete)
- Unique: (screen_id, row_label, seat_number)
```

#### 9. **showtimes** - Suất chiếu
```sql
- id (PK)
- movie_id (FK → movies)
- screen_id (FK → screens)
- show_date
- start_time, end_time
- price (base price)
- status (ACTIVE/INACTIVE/CANCELED)
- Unique: (screen_id, show_date, start_time)
```

#### 10. **bookings** - Đơn đặt vé
```sql
- id (PK)
- account_id (FK → accounts)
- showtime_id (FK → showtimes)
- total_price
- status (PENDING_PAYMENT/CONFIRMED/CANCELLED/EXPIRED)
- booking_date
- created_at, updated_at
```

#### 11. **booking_seats** - Ghế đã đặt
```sql
- id (PK)
- booking_id (FK → bookings)
- seat_id (FK → seats)
- price (actual price paid)
```

### Indexes
Tất cả foreign keys đều có index. Các index bổ sung:
- `idx_account_username`, `idx_account_email`
- `idx_movie_title`, `idx_movie_status`, `idx_movie_genre`
- `idx_showtime_date`, `idx_showtime_start_time`
- `idx_booking_status`
- `idx_seat_screen`, `idx_seat_row_seat`

---

## 🔄 Flow nghiệp vụ

### 1. 📝 Registration Flow
```
1. User POST /api/auth/register
2. System validate input (username/email unique)
3. Create account (status=ACTIVE, email_verified=false)
4. Send OTP via email (TTL: 5 phút)
5. User POST /api/auth/activate với OTP
6. System verify OTP → set email_verified=true
7. Success ✅
```

**[Chi tiết xem: FLOWS.md - Registration Flow](#)**

### 2. 🔐 Login Flow
```
1. User POST /api/auth/login (username + password)
2. Spring Security authenticate
3. Validate email_verified=true & status=ACTIVE
4. Generate JWT access token (TTL: 30 phút)
5. Generate refresh token (TTL: 1 ngày)
6. Store refresh token in Redis
7. Return both tokens ✅
```

### 3. 🎫 Booking Flow (CRITICAL)
```
┌─────────────────────────────────────────────────────────┐
│ STEP 1: Hold Seats (Tạm giữ ghế)                        │
└─────────────────────────────────────────────────────────┘
1. User chọn ghế trên UI
2. Frontend gọi POST /api/seats/hold
   - showtimeId, seatIds[], ttlSec (default 120)
3. Backend validate:
   - Showtime exists
   - Seats exist
4. Redis SETNX cho từng ghế:
   - Key: "hold:{showtimeId}:{seatId}"
   - Value: userId
   - TTL: 120 seconds
5. Nếu ghế đã bị hold → ConflictException
6. Success → User có 120s để booking

┌─────────────────────────────────────────────────────────┐
│ STEP 2: Create Booking (Tạo đơn đặt vé)                 │
└─────────────────────────────────────────────────────────┘
7. User gọi POST /api/bookings trong 120s
8. Backend:
   a. Verify seats STILL held by user (assertHeldByUser)
   b. Get seat infos (VIP/STANDARD)
   c. Acquire distributed locks (sorted order)
   d. Re-verify holds under lock (TOCTOU prevention)
   e. Check DB: ghế chưa bị booking
   f. Calculate total price:
      - STANDARD: basePrice
      - VIP: basePrice * 1.3
   g. Create booking (status=PENDING_PAYMENT)
   h. Save booking_seats
   i. Consume Redis holds (delete keys)
   j. Release locks
9. Return bookingId & totalPrice
10. User MUST pay within 15 phút

┌─────────────────────────────────────────────────────────┐
│ STEP 3: Payment (Thanh toán)                            │
└─────────────────────────────────────────────────────────┘
11. Frontend redirect sang POST /api/payments/create/{bookingId}
12. Backend generate payment URL (⚠️ MOCK - cần integrate gateway)
13. User thanh toán trên gateway
14. Gateway callback POST /api/payments/callback
15. Backend:
    - ⚠️ MUST verify signature (chưa implement!)
    - Validate amount matches booking.totalPrice
    - Update booking.status = CONFIRMED
    - Send confirmation email
16. Success ✅

┌─────────────────────────────────────────────────────────┐
│ STEP 4: Auto-Expire (Tự động hủy đơn quá hạn)           │
└─────────────────────────────────────────────────────────┘
17. Cron job chạy mỗi 5 phút (BookingExpireService)
18. Find bookings: status=PENDING_PAYMENT & > 15 phút
19. Update status = EXPIRED
20. Release Redis holds (nếu còn)
21. Seats available cho user khác ✅
```

**[Chi tiết đầy đủ: FLOWS.md](#)**

---

## 🔐 Security

### Authentication
- **JWT (JSON Web Token)** với HMAC-SHA256
- Access token: 30 phút
- Refresh token: 1 ngày (stored in Redis)

### Authorization
- **Role-based access control (RBAC)**
- Roles: USER, ADMIN, THEATER_MANAGEMENT
- `@PreAuthorize` annotations trên controllers

### Password Security
- BCrypt hashing với salt
- Minimum 8 characters
- Forgot password với OTP verification

### API Security
- CORS configured (only localhost:3000)
- Stateless sessions (no cookies)
- JWT filter runs before every request

### Data Validation
- `@Valid` annotations
- Bean Validation (JSR-303)
- Custom validators

---

## 🧪 Testing

### Run tests
```bash
mvn test
```

### Run with coverage
```bash
mvn clean test jacoco:report
```
Report: `target/site/jacoco/index.html`

### Test structure
```
src/test/java/
├── config/         - Test configurations
├── controller/     - Controller integration tests
├── service/        - Service unit tests
├── repository/     - Repository tests
└── security/       - Security tests
```

### Postman Collection
Import file: `Movie_Booking_System_API_Tests.postman_collection.json`

---

## ⚠️ Issues & Improvements

### 🚨 CRITICAL Issues

#### 1. **Payment Gateway chưa integrate**
- ❌ Payment URL là MOCK
- ❌ Không verify signature từ gateway
- ❌ Risk: Attacker có thể fake payment success!

**Fix:**
- Integrate VNPay/MoMo/Stripe
- Implement signature verification
- Add idempotency check (transaction ID)

#### 2. **Thiếu Webhook endpoint cho Payment**
- ❌ Chỉ dựa vào callback (user có thể đóng browser)
- ❌ Không handle async notifications từ gateway

**Fix:**
- Add POST /api/payments/webhook
- Verify webhook signature
- Process payment status updates

#### 3. **Booking update/delete không nên support**
- ❌ `BookingController.update()` & `delete()` throw UnsupportedOperationException
- ⚠️ Nhưng vẫn có endpoints

**Fix:**
- Remove endpoints hoặc implement proper cancellation flow
- Cancellation chỉ qua payment flow

### 🔶 MAJOR Issues

#### 4. **Pagination chưa implement đầy đủ**
- ❌ `BookingService.getAlls()` throw UnsupportedOperationException
- ❌ Thiếu filters (by user, by status, by date)

**Fix:**
- Implement với Spring Data Pageable
- Add filters với Specification API

#### 5. **Email templates quá đơn giản**
- ⚠️ Chưa có template cho booking confirmation
- ⚠️ Chưa có QR code cho vé

**Fix:**
- Design HTML email templates
- Generate QR code với booking ID

#### 6. **Thiếu transaction management một số nơi**
- ⚠️ Một số service methods không có `@Transactional`
- Risk: Inconsistent data khi có exception

**Fix:**
- Review và thêm `@Transactional` cho business methods

#### 7. **Redis connection pool chưa configure**
- ⚠️ Sử dụng default settings
- Risk: Performance issue khi high traffic

**Fix:**
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### 🔷 MINOR Issues

#### 8. **Logging chưa structured**
- ⚠️ Log format không consistent
- ⚠️ Thiếu correlation ID cho distributed tracing

**Fix:**
- Use Logback MDC
- Add correlation ID filter

#### 9. **Error messages hardcoded**
- ⚠️ Error messages trong code (không i18n)

**Fix:**
- Externalize messages vào messages.properties

#### 10. **Thiếu API rate limiting**
- ⚠️ Chỉ có OTP rate limit (Redis-based)
- ⚠️ Các API khác không có rate limit

**Fix:**
- Implement với Bucket4j + Redis

#### 11. **Chưa có health check endpoints**
- ⚠️ Actuator chỉ expose health,info,metrics
- ⚠️ Chưa check Redis, MySQL availability

**Fix:**
- Custom HealthIndicator cho Redis & MySQL

#### 12. **Soft delete không consistent**
- ✅ Seat có soft delete (`is_deleted`)
- ❌ Các entity khác không có

**Fix:**
- Implement soft delete cho Movie, Theater, etc.
- Hoặc remove soft delete nếu không cần

---

## 📁 Project Structure

```
src/main/java/com/trainning/movie_booking_system/
├── config/                      # Configuration classes
│   ├── JacksonConfig.java       # JSON serialization
│   ├── MailConfig.java          # Email config
│   ├── OpenApiConfig.java       # Swagger config
│   ├── RedisConfig.java         # Redis config
│   └── SecurityConfig.java      # Security & JWT config
│
├── controller/                  # REST Controllers
│   ├── AuthController.java      # /api/auth/*
│   ├── BookingController.java   # /api/bookings/*
│   ├── MovieController.java     # /api/movies/*
│   ├── PaymentController.java   # /api/payments/*
│   ├── SeatController.java      # /api/seats/*
│   ├── SeatHoldController.java  # /api/seats/hold
│   └── ShowtimeController.java  # /api/showtimes/*
│
├── dto/                         # Data Transfer Objects
│   ├── request/                 # Request DTOs
│   └── response/                # Response DTOs
│
├── entity/                      # JPA Entities
│   ├── Account.java
│   ├── User.java
│   ├── Role.java
│   ├── Movie.java
│   ├── Theater.java
│   ├── Screen.java
│   ├── Seat.java
│   ├── Showtime.java
│   ├── Booking.java
│   └── BookingSeat.java
│
├── exception/                   # Exception handling
│   ├── GlobalExceptionHandle.java
│   ├── BadRequestException.java
│   ├── NotFoundException.java
│   └── ConflictException.java
│
├── helper/                      # Helper services
│   ├── cron/
│   │   └── BookingExpireService.java  # Auto-expire bookings
│   ├── redis/
│   │   ├── SeatDomainService.java     # Seat hold/release
│   │   └── RedisLockService.java      # Distributed locks
│   └── specification/                 # JPA Specifications
│
├── mapper/                      # MapStruct mappers
│   ├── AuthMapper.java
│   ├── BookingMapper.java
│   └── MovieMapper.java
│
├── repository/                  # JPA Repositories
│   ├── AccountRepository.java
│   ├── BookingRepository.java
│   ├── MovieRepository.java
│   └── ...
│
├── security/                    # Security components
│   ├── JwtProvider.java         # JWT generation/validation
│   ├── JwtFilter.java           # JWT authentication filter
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java       # Utility methods
│
├── service/                     # Business logic services
│   ├── impl/                    # Service implementations
│   └── Movie/
│       ├── MovieService.java
│       └── MovieSearchService.java
│
└── untils/enums/                # Enums
    ├── BookingStatus.java
    ├── MovieStatus.java
    ├── RoleType.java
    └── ...
```

---

## 🤝 Contributing

1. Fork repository
2. Create branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -m 'Add feature'`
4. Push: `git push origin feature/your-feature`
5. Create Pull Request

---

## 📝 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author
https://github.com/hoandevv
---

**⚡️ Next Steps:**
1. ✅ Read [Documentation Index](docs/README.md) for organized guides
2. � Follow [Setup Guide](docs/01-SETUP-GUIDE.md) to run the project
3. 🎫 Understand [Booking Flow](docs/04-BOOKING-FLOW.md) - **Fixed with concurrency control**
4. � Understand [Payment Flow](docs/05-PAYMENT-FLOW.md) - **Fixed with idempotency**
5. ✅ Check [TODO List](docs/06-TODO.md) for pending improvements
6. 🧪 Import Postman collection to test APIs

**🗂️ Old docs archived:** See [.archive/README.md](.archive/README.md)
