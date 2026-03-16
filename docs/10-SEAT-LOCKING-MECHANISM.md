# Cơ Chế Khóa Ghế Tạm Thời (Seat Hold/Locking Mechanism)

Tài liệu này mô tả chi tiết cách hệ thống xử lý việc giữ ghế tạm thời cho người dùng trong quá trình đặt vé để tránh tình trạng tranh chấp (race condition) và đặt trùng ghế (double booking).

---

## 1. Tổng quan Nghiệp vụ (Business Logic)

### Mục đích
Khi người dùng bắt đầu chọn ghế, hệ thống cần "khóa" những ghế đó lại trong một khoảng thời gian ngắn (mặc định 120 giây) để người dùng có thể thực hiện các bước tiếp theo (nhập thông tin, chọn voucher, thanh toán) mà không bị người khác chiếm mất ghế.

### Các Quy tắc Nghiệp vụ (Business Rules)
- **Tính hợp lệ của Suất chiếu:**
    - Không thể giữ ghế cho suất chiếu đã bắt đầu.
    - Hệ thống ngừng cho giữ ghế trước giờ chiếu 15 phút (Cutoff time).
- **Trạng thái vật lý của Ghế:**
    - Ghế phải đang ở trạng thái sẵn sàng (không thuộc diện `MAINTENANCE` - Bảo trì).
- **Kiểm tra trùng lặp:**
    - Ghế chưa được đặt thành công (`CONFIRMED`) trong Database.
    - Ghế không nằm trong các đơn hàng đang chờ thanh toán (`PENDING_PAYMENT`).
    - Ghế không bị người dùng khác giữ trong Redis.
- **Cơ chế TTL (Time-To-Live):**
    - Thời gian giữ ghế mặc định là 120 giây.
    - Nếu cùng một người dùng giữ lại ghế đó, thời gian hết hạn (TTL) sẽ được làm mới (refresh).
- **Sở hữu (Ownership):**
    - Chỉ người dùng đang giữ ghế mới có quyền thực hiện đặt vé cho những ghế đó.

---

## 2. Giải pháp Công nghệ (Implementation)

Hệ thống sử dụng kết hợp giữa **PostgreSQL (Database)** để kiểm tra trạng thái lâu dài và **Redis** để quản lý trạng thái tạm thời với tốc độ cao.

### Thành phần chính
- **Redis (StringRedisTemplate):** Lưu trữ thông tin khóa ghế.
- **Key Format:** `hold:{showtimeId}:{seatId}`
- **Value:** `{userId}` (Lưu ID người dùng để xác thực quyền sở hữu).

### Các thao tác kỹ thuật
- **Atomic Hold (NX - Not Exists):** Sử dụng lệnh `SETNX` (thông qua `redis.opsForValue().setIfAbsent`) để đảm bảo tại một thời điểm chỉ một người có thể tạo khóa cho một ghế cụ thể một cách nguyên tử.
- **TTL (Expiration):** Tự động xóa khóa trong Redis khi hết thời gian mà không cần tác động thủ công, giúp ghế tự động "mở" lại nếu người dùng thoát trình duyệt hoặc không thanh toán.
- **Rollback:** Nếu việc giữ một danh sách ghế bị lỗi giữa chừng (ví dụ giữ được 2/3 ghế), hệ thống sẽ thực hiện xóa các khóa đã tạo trước đó để đảm bảo tính nhất quán.

---

## 3. Quy trình thực hiện (Workflow)

### Bước 1: Yêu cầu giữ ghế (Hold Seats)
1. User gửi danh sách `seatIds` và `showtimeId`.
2. Hệ thống kiểm tra Database:
    - Suất chiếu có hợp lệ không?
    - Ghế có đang bảo trì không?
    - Ghế đã được ai đặt/thanh toán chưa?
3. Hệ thống kiểm tra Redis:
    - Nếu ghế chưa có key -> Tạo key với TTL.
    - Nếu ghế đã có key và là của chính User đó -> Cập nhật lại TTL.
    - Nếu ghế của người khác -> Báo lỗi `ConflictException`.

### Bước 2: Tạo đơn hàng (Create Booking)
1. Hệ thống gọi hàm `assertHeldByUser` để kiểm tra lại một lần cuối trong Redis.
2. Nếu key trong Redis đã hết hạn hoặc thuộc về người khác -> Hủy quá trình tạo đơn hàng.
3. Nếu hợp lệ -> Tiến hành tạo bản ghi Booking trong DB.

### Bước 3: Giải phóng ghế (Consume/Release)
- **Thành công:** Sau khi Booking được tạo hoặc thanh toán thành công, hệ thống gọi `consumeHoldToBooked` để xóa key trong Redis (vì lúc này ghế đã được bảo vệ bởi trạng thái trong DB).
- **Hủy thủ công:** User có thể gọi API `DELETE /api/v1/seat-holds` để chủ động nhả ghế.
- **Timeout:** Redis tự động xóa key, ghế trở lại trạng thái trống cho người khác.

---

## 4. Các File liên quan trong Project
- **Controller:** `SeatHoldController.java`
- **Service:** `SeatHoldService.java` & `SeatHoldServiceImpl.java`
- **Helper:** `SeatDomainService.java` (Chứa logic cốt lõi xử lý Redis)
- **Config:** `RedisConfig.java`
