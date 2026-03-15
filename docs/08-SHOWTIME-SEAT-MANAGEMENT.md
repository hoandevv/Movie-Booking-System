# 🗓️ Showtime & Seat Management Guide

> **Last Updated:** March 13, 2026  
> **Status:** ✅ Implemented

---

## 📋 OVERVIEW

Module này giải quyết bài toán phức tạp nhất trong rạp chiếu phim: **Lịch chiếu** và **Sơ đồ ghế**.
- 🗓️ **Showtimes**: Lịch chiếu cụ thể của một bộ phim tại một phòng chiếu.
- 💺 **Seat Generation**: Tự động tạo hàng trăm ghế cho một phòng chiếu dựa trên cấu hình (Rows x Cols).
- 💎 **Seat Types**: Phân loại ghế (STANDARD, VIP) và giá tiền tương ứng.

---

## 🏗️ DATA STRUCTURE

### 1. Showtime Entity
- **Fields:** StartTime, EndTime (tính dựa trên duration phim), BasePrice.
- **Status:** `ACTIVE`, `CANCELLED`, `FINISHED`.
- **Relationship:** Liên kết Movie (1) và Screen (1).

### 2. Seat Entity
- **Fields:** SeatRow, SeatNumber, SeatType, ScreenId.
- **SeatType:** `STANDARD`, `VIP`, `DELUXE`.

---

## 🚀 MANAGEMENT FLOW

### 1. Showtime Setup Flow
```
Admin Select Movie & Screen
   ↓
Check Conflict (Screen availability at that time)
   ↓
Calculate EndTime (StartTime + MovieDuration + 15min cleaning)
   ↓
Save Showtime
```

### 2. Auto-Generate Seats Flow
Admin không cần tạo từng ghế. Hệ thống cung cấp API để generate hàng loạt:
```
Input: screenId, rows (e.g., 10), seatsPerRow (e.g., 12)
   ↓
System loops: Row A -> J, Column 1 -> 12
   ↓
Determine SeatType (e.g., middle rows are VIP)
   ↓
Bulk Insert into Database
```

---

## 🔗 API ENDPOINTS

### 🗓️ Showtime APIs (`ShowtimeController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/showtimes` | Public | Danh sách suất chiếu (theo phim/rạp/ngày) |
| `GET` | `/api/v1/showtimes/{id}` | Public | Chi tiết suất chiếu |
| `POST` | `/api/v1/showtimes` | Admin | Tạo suất chiếu mới (Kiểm tra trùng lịch) |

### 💺 Seat APIs (`SeatController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/seats/generate/{screenId}` | Admin | **(CORE)** Tự động tạo danh sách ghế |
| `GET` | `/api/v1/seats/screen/{screenId}` | Public | Lấy sơ đồ ghế của phòng chiếu |
| `PUT` | `/api/v1/seats/{id}/type` | Admin | Cập nhật loại ghế (Ví dụ: Đổi sang VIP) |

---

## 🛠️ TECHNICAL DETAILS

### Seat Generation Logic
Trong `SeatServiceImpl.java`, hệ thống xử lý đặt tên ghế thông minh:
```java
// Logic: A1, A2, ..., B1, B2...
for (int r = 0; r < request.getTotalRows(); r++) {
    char rowChar = (char) ('A' + r);
    for (int c = 1; c <= request.getSeatsPerRow(); c++) {
        Seat seat = new Seat();
        seat.setSeatRow(String.valueOf(rowChar));
        seat.setSeatNumber(c);
        // VIP logic: Nếu là 4 hàng giữa
        if (r >= 3 && r <= 6) seat.setSeatType(SeatType.VIP);
        else seat.setSeatType(SeatType.STANDARD);
        // ... save
    }
}
```

### Conflict Detection (Tránh trùng lịch)
Hệ thống sử dụng query để kiểm tra xem Screen đó đã có phim nào chiếu trong khoảng thời gian dự kiến chưa:
```sql
SELECT count(*) FROM showtimes 
WHERE screen_id = :id 
AND (:start BETWEEN start_time AND end_time 
     OR :end BETWEEN start_time AND end_time)
```

---

## 🧪 TESTING SCENARIOS

1. **Tạo lịch chiếu trùng:** Hệ thống phải báo lỗi nếu phòng chiếu đang bận.
2. **Generate Seats:** Thử tạo 10x10 ghế -> Kiểm tra Database có đúng 100 bản ghi không.
3. **Giá vé:** Kiểm tra giá vé suất chiếu VIP phải cao hơn giá vé cơ bản (BasePrice * Multiplier).

---

## 📚 RELATED DOCS

- [Booking Flow](04-BOOKING-FLOW.md) - Cách User đặt vé dựa trên lịch chiếu.
- [Database Schema](../PROJECT_STRUCTURE.md) - Chi tiết bảng Showtimes và Seats.

---
