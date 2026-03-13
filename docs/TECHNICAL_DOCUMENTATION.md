# TECHNICAL DOCUMENTATION: MOVIE BOOKING SYSTEM BACKEND

## 1. TỔNG QUAN HỆ THỐNG (SYSTEM OVERVIEW)

### 1.1 Mục tiêu
Hệ thống cung cấp nền tảng backend cho ứng dụng đặt vé xem phim trực tuyến, hỗ trợ đa nền tảng (Web/Mobile). Hệ thống tập trung vào khả năng xử lý đồng thời cao (high concurrency) cho việc đặt ghế, tích hợp thanh toán an toàn và quản lý lịch chiếu linh hoạt.

### 1.2 Kiến trúc tổng thể
*   **Architecture Pattern:** Layered Monolithic Architecture (Controller ↔ Service ↔ Repository).
*   **Design Principles:** RESTful API, Stateless (JWT), Fail-fast validation.

### 1.3 Tech Stack & Hạ tầng
| Hạng mục | Công nghệ / Thư viện | Phiên bản | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 (LTS) | |
| **Framework** | Spring Boot | 3.3.4 | Core framework |
| **Database** | MySQL | 8.0+ | Lưu trữ dữ liệu bền vững (RDBMS) |
| **Cache/Lock** | Redis | Latest | Quản lý Seat Holding & Distributed Lock |
| **ORM** | Spring Data JPA | - | Hibernate implementation |
| **Security** | Spring Security | 6.4.11 | JWT Authentication & Authorization |
| **Payment** | VNPay SDK | - | Cổng thanh toán nội địa |
| **Build Tool** | Maven | - | Dependency Management |
| **Utils** | Lombok, MapStruct | - | Giảm boilerplate code |

---

## 2. LUỒNG NGHIỆP VỤ CHÍNH (BUSINESS FLOWS)

### 2.1 Authentication Flow
Người dùng phải xác thực để thực hiện các hành động bảo mật.
1.  **Register:** User gửi thông tin → Hệ thống tạo tài khoản (status: `UNVERIFIED`) → Gửi OTP qua email.
2.  **Activate:** User nhập OTP → Hệ thống kiểm tra → Update status: `ACTIVE`.
3.  **Login:** User gửi username/password → Hệ thống xác thực → Trả về `AccessToken` (ngắn hạn) và `RefreshToken` (dài hạn).
4.  **Authorized Request:** Client gửi AccessToken trong header `Authorization: Bearer <token>` → `JwtFilter` xác thực.

### 2.2 Booking Flow (Critical Path)
Quy trình phức tạp nhất nhằm đảm bảo tính toàn vẹn dữ liệu khi nhiều người cùng chọn một ghế.
1.  **Select Seat:** Client gọi `GET /seats` để xem trạng thái ghế.
2.  **Hold Seat:** Client gọi `POST /seats/hold` → Hệ thống lưu trạng thái giữ ghế vào Redis (TTL 5-10 phút).
3.  **Create Booking:** Client gọi `POST /bookings` → Hệ thống kiểm tra Redis Hold → Tạo Booking (Status: `PENDING_PAYMENT`).
4.  **Payment:** Client gọi API thanh toán → Redirect sang VNPay.

### 2.3 Payment Flow
1.  **Initiate:** Từ Booking ID → Tạo URL thanh toán VNPay.
2.  **User Action:** Người dùng thanh toán trên trang VNPay.
3.  **Callback (IPN/Return):** VNPay gọi về Backend → Verify Signature → Update Booking Status.

---

## 3. PHÂN TÍCH CHI TIẾT API QUAN TRỌNG

### 3.1 Hold Seats API (`SeatHoldController`)
Dùng để giữ chỗ tạm thời, ngăn người khác chọn cùng lúc.
*   **Endpoint:** `POST /api/v1/seats/hold`
*   **Service:** `SeatHoldServiceImpl`
*   **Logic:**
    1.  Validate `showtimeId` và danh sách `seatIds`.
    2.  Gọi `SeatDomainService` để lưu key vào Redis theo format: `seat_hold:{showtimeId}:{seatId}`.
    3.  Set TTL (Time-To-Live) mặc định 120s hoặc theo config.
*   **Redis Interaction:** Sử dụng `RedisTemplate` để set key.

### 3.2 Create Booking API (`BookingController`)
*   **Endpoint:** `POST /api/v1/bookings`
*   **Service:** `BookingServiceImpl`
*   **Transaction:** `@Transactional`
*   **Logic chi tiết:**
    1.  **Validate Input:** Kiểm tra `seatIds` không rỗng, `showtime` hợp lệ (không phải quá khứ).
    2.  **Verify Hold:** Kiểm tra trong Redis xem ghế có đang được giữ bởi chính User này không (`seatClient.assertHeldByUser`).
    3.  **Distributed Lock:** Dùng Redis Lock (`redisLockService.tryLockSeat`) khóa từng ghế để tránh race condition ở mức mili-giây.
    4.  **Calculate Price:** Tính tổng tiền dựa trên loại ghế (VIP/Normal) và giá suất chiếu.
    5.  **Apply Voucher:** Gọi `VoucherService` để validate và trừ tiền nếu có voucher.
    6.  **DB Save:** Lưu `Booking` (PENDING_PAYMENT) và `BookingSeat`.
    7.  **Consume Hold:** Chuyển trạng thái ghế trong Redis từ "Hold" sang "Booked".
    8.  **Release Lock:** Giải phóng Distributed Lock.

### 3.3 Create Payment API (`PaymentController`)
*   **Endpoint:** `GET /api/v1/payments/create-payment`
*   **Service:** `PaymentServiceImpl`
*   **Logic:**
    1.  Lấy thông tin Booking.
    2.  Validate trạng thái phải là `PENDING_PAYMENT`.
    3.  Tạo bản ghi `PaymentTransaction` (Status: `PENDING`) để log lại lịch sử.
    4.  Gọi `VnPayService` để tạo URL redirect với checksum SHA512.

---

## 4. LUỒNG DỮ LIỆU NỘI BỘ & CƠ CHẾ KHÓA (INTERNAL FLOW)

### 4.1 Cơ chế khóa 2 lớp (Two-Layer Locking Strategy)
Hệ thống sử dụng chiến lược khóa kết hợp để đảm bảo hiệu năng và tính đúng đắn:
1.  **Lớp 1: Optimistic Locking via Redis TTL (Seat Holding):**
    *   Khi User chọn ghế, hệ thống "đánh dấu" chủ quyền trong Redis trong 5 phút.
    *   Giúp giảm tải cho Database, User khác thấy ghế đã được chọn ngay lập tức.
2.  **Lớp 2: Pessimistic/Distributed Lock (Booking Creation):**
    *   Tại thời điểm bấm "Thanh toán" (tạo Booking), hệ thống dùng Redis Distributed Lock để khóa cứng ID ghế.
    *   Đảm bảo ngay cả khi 2 request vượt qua lớp 1 cùng lúc, chỉ 1 request tạo được Booking vào DB.

### 4.2 Xử lý hết hạn (Expiration Handling)
*   **Redis Key Expiration:** Ghế tự động nhả ra sau khi hết TTL nếu không tạo Booking.
*   **Booking Expiration (Cron Job):** `PaymentServiceImpl.releaseExpiredBookings` chạy mỗi 60s.
    *   Quét các Booking `PENDING_PAYMENT`.
    *   Nếu tạo quá 15 phút mà chưa có Transaction thành công → Update status `EXPIRED` → Release ghế.

---

## 5. DATABASE DESIGN (KEY ENTITIES)

### 5.1 Booking & Seats
*   **Booking:**
    *   `id` (PK), `account_id` (FK), `showtime_id` (FK).
    *   `total_price`, `discount_amount`, `final_amount`.
    *   `status`: PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED.
    *   `expires_at`: Thời gian hết hạn thanh toán.
*   **BookingSeat:**
    *   Liên kết n-n giữa Booking và Seat (snapshot giá và trạng thái tại thời điểm đặt).
    *   `price`: Giá vé tại thời điểm mua (tránh việc giá gốc thay đổi ảnh hưởng lịch sử).

### 5.2 Payment
*   **PaymentTransaction:**
    *   Lưu lịch sử mọi lần thử thanh toán (1 Booking có thể có nhiều Transaction nếu user thử lại).
    *   `transaction_id`: Mã tham chiếu gửi sang VNPay.
    *   `gateway_type`: VNPAY.
    *   `status`: PENDING, SUCCESS, FAILED.

---

## 6. CƠ CHẾ ĐẢM BẢO TÍNH NHẤT QUÁN DỮ LIỆU

### 6.1 Race Condition Protection
Vấn đề: 2 User cùng bấm giữ ghế A cùng lúc.
*   **Giải pháp:** Redis `SETNX` (Set if Not Exists). Chỉ 1 lệnh thành công, lệnh kia trả về false.

### 6.2 Transaction Boundaries
*   Tất cả các thao tác ghi vào DB (Save Booking + Save BookingSeats) đều nằm trong `@Transactional`.
*   Nếu có lỗi xảy ra (ví dụ: lỗi mạng, lỗi validate voucher), toàn bộ dữ liệu DB sẽ rollback, không có chuyện Booking được tạo mà không có ghế.

---

## 7. PAYMENT INTEGRATION (VNPAY)

### 7.1 Security Validation
*   **Checksum:** Mọi request từ VNPay đều kèm theo `vnp_SecureHash`.
*   **Verification:** `VnPayService.verifySignature` sử dụng thuật toán HMAC SHA512 và `vnp_HashSecret` (trong config) để hash lại dữ liệu và so sánh. Nếu không khớp → Từ chối xử lý (tránh hacker giả mạo request thanh toán thành công).

### 7.2 Idempotency (Tính lũy đẳng)
*   Callback từ VNPay có thể bị gọi nhiều lần (do mạng retry).
*   **Logic:** Kiểm tra `transaction.getStatus()`.
    *   Nếu đã là `SUCCESS` hoặc `FAILED` → Bỏ qua, trả về kết quả cũ ngay lập tức.
    *   Chỉ xử lý nếu đang là `PENDING`.

---

## 8. CÁC EDGE CASES QUAN TRỌNG

1.  **User giữ ghế nhưng không thanh toán:**
    *   Redis TTL hết hạn → Ghế trống trên UI.
    *   Booking đã tạo nhưng chưa trả tiền → Cron Job quét sau 15p → Huỷ Booking.
2.  **Callback thanh toán bị lỗi/chậm:**
    *   User trả tiền rồi nhưng Backend chưa nhận được IPN.
    *   Booking vẫn là `PENDING`.
    *   User cần chức năng "Check Status" thủ công hoặc Cron Job đối soát với VNPay (cần implement thêm API Query DR của VNPay).
3.  **Voucher hết lượt dùng khi đang thanh toán:**
    *   Lúc apply thì còn, lúc submit thanh toán thì hết.
    *   Cần lock voucher hoặc check lại số lượng ngay trước khi `bookingRepository.save()`.

---

## 9. ĐỀ XUẤT CẢI TIẾN HỆ THỐNG

1.  **Scalability:** Tách `Booking Service` và `Payment Service` thành Microservices riêng biệt, giao tiếp qua Kafka/RabbitMQ để xử lý việc gửi Email vé và Log thanh toán không đồng bộ, giảm độ trễ cho User.
2.  **Distributed Lock Optimization:** Sử dụng Redlock (thuật toán của Redis) thay vì `set key` đơn giản để đảm bảo an toàn hơn trong môi trường Redis Cluster.
3.  **Client-side Polling/WebSocket:** Khi thanh toán xong trên mobile/web, thay vì đợi redirect, Client nên lắng nghe qua WebSocket để nhận thông báo "Vé đã đặt thành công" ngay lập tức (Real-time update).
