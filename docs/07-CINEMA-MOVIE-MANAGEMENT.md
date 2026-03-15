# 🎬 Cinema & Movie Management Guide

> **Last Updated:** March 13, 2026  
> **Status:** ✅ Implemented

---

## 📋 OVERVIEW

Module này quản lý các thực thể nền tảng của hệ thống đặt vé, bao gồm:
- 🎥 **Movies**: Thông tin phim, thời lượng, thể loại và trạng thái chiếu.
- 🏢 **Theaters**: Cụm rạp, địa chỉ và thông tin liên hệ.
- 📺 **Screens**: Phòng chiếu thuộc rạp, định dạng (2D/3D/IMAX) và tổng số ghế.

---

## 🏗️ DATA STRUCTURE

### 1. Movie Entity
Quản lý vòng đời của bộ phim từ khi sắp chiếu đến khi kết thúc.
- **Status:** `COMING_SOON`, `NOW_SHOWING`, `ENDED`.
- **Fields:** Title, Description, Duration (minutes), ReleaseDate, PosterUrl, TrailerUrl, Rating.

### 2. Theater Entity
Đại diện cho một cụm rạp vật lý.
- **Fields:** Name, Address, City, Hotline.
- **Relationship:** Một Theater có nhiều Screens.

### 3. Screen Entity
Phòng chiếu cụ thể trong rạp.
- **Type:** `SCREEN_2D`, `SCREEN_3D`, `SCREEN_IMAX`.
- **Fields:** Name, TotalSeats, Status (ACTIVE/INACTIVE).

---

## 🚀 MANAGEMENT FLOW

### 1. Movie Lifecycle Flow
```
Admin Create Movie (Status: COMING_SOON)
   ↓
Update Trailer/Poster
   ↓
Create Showtimes (Status automatically becomes NOW_SHOWING)
   ↓
Movie ends its run (Manual or Auto update to ENDED)
```

### 2. Theater & Screen Setup
```
Create Theater (e.g., CGV Vincom)
   ↓
Add Screens to Theater (e.g., Screen 01, Screen 02)
   ↓
Generate Seats for each Screen (See Showtime & Seat Management)
```

---

## 🔗 API ENDPOINTS

### 🎥 Movie APIs (`MovieController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/movies` | Public | Lấy danh sách phim (có phân trang/filter) |
| `GET` | `/api/v1/movies/{id}` | Public | Chi tiết phim |
| `POST` | `/api/v1/movies` | Admin | Thêm phim mới |
| `PUT` | `/api/v1/movies/{id}` | Admin | Cập nhật thông tin phim |
| `DELETE` | `/api/v1/movies/{id}` | Admin | Xóa phim (Soft delete) |

### 🏢 Theater APIs (`TheaterController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/theaters` | Public | Danh sách cụm rạp |
| `GET` | `/api/v1/theaters/{id}/screens` | Public | Danh sách phòng chiếu của rạp |
| `POST` | `/api/v1/theaters` | Admin | Tạo cụm rạp mới |

### 📺 Screen APIs (`ScreenController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/screens/{id}` | Public | Thông tin phòng chiếu |
| `POST` | `/api/v1/screens` | Admin | Thêm phòng chiếu vào rạp |
| `PUT` | `/api/v1/screens/{id}` | Admin | Cập nhật cấu hình phòng chiếu |

---

## 🛠️ TECHNICAL DETAILS

### Movie Filter Logic
Hệ thống sử dụng **Spring Data Specification** để filter phim linh hoạt:
```java
// Ví dụ filter phim đang chiếu tại một thành phố cụ thể
public List<Movie> findMovies(String city, MovieStatus status) {
    return movieRepository.findAll(Specification
        .where(hasStatus(status))
        .and(availableInCity(city)));
}
```

### Image Storage
Hiện tại hệ thống lưu trữ URL (PosterUrl/TrailerUrl). Khuyến nghị tích hợp Cloudinary hoặc AWS S3 trong tương lai.

---

## 🧪 TESTING SCENARIOS

1. **Admin tạo phim mới:** Kiểm tra validation các trường bắt buộc (Title, Duration).
2. **User tìm kiếm phim:** Filter theo thể loại và trạng thái `NOW_SHOWING`.
3. **Xóa Rạp:** Đảm bảo không thể xóa rạp nếu đang có lịch chiếu hoạt động (Constraint check).

---

## 📚 RELATED DOCS

- [Showtime & Seat Management](08-SHOWTIME-SEAT-MANAGEMENT.md) - Cách tạo suất chiếu cho phim.
- [API Documentation](02-API-DOCUMENTATION.md) - Chi tiết request/response body.

---
