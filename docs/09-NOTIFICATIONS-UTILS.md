# 📧 Notifications & Utils Guide

> **Last Updated:** March 13, 2026  
> **Status:** ✅ Implemented

---

## 📋 OVERVIEW

Hệ thống hỗ trợ các công cụ bổ trợ (Utility) để nâng cao trải nghiệm người dùng và bảo mật:
- ✉️ **Mail Service**: Gửi Email cho OTP, Xác nhận đặt vé, và Quên mật khẩu.
- 🔐 **OTP Service**: Quản lý mã OTP với cơ chế giới hạn tốc độ (Rate Limiting).
- 🚀 **Redis**: Sử dụng làm Cache layer cho Seat Holding và OTP Cooldown.

---

## 🏗️ SYSTEM COMPONENTS

### 1. Mail Service
Sử dụng **Spring Boot Starter Mail** tích hợp Gmail SMTP.
- **Templates:** Sử dụng **Thymeleaf** để generate nội dung email HTML chuyên nghiệp.
- **Features:** Gửi đính kèm (Attachment) cho vé điện tử (Future).

### 2. OTP Service
Tạo mã OTP 6 số ngẫu nhiên cho xác thực.
- **OTP Types:** `REGISTER`, `FORGOT_PASSWORD`, `CHANGE_EMAIL`.
- **Storage:** Lưu vào Redis với TTL (Time-to-Live).

### 3. Redis Utils
Lớp bọc (Wrapper) cho `StringRedisTemplate`.
- **Key Patterns:**
    - `otp:{email}` - Giá trị OTP.
    - `otp_cooldown:{email}` - Cooldown để chặn spam (60s).
    - `seat_hold:{showtimeId}:{seatId}` - Khóa ghế tạm thời.

---

## 🚀 FLOWS & LOGIC

### 1. OTP Rate Limiting Flow
Ngăn chặn người dùng bấm "Gửi lại" liên tục gây lãng phí tài nguyên và spam.
```
User request OTP
   ↓
Check "otp_cooldown:{email}" in Redis
   ↓
If exists -> Throw 429 Too Many Requests (Wait 60s)
   ↓
If NOT exists -> Generate OTP + Send Mail
   ↓
Set "otp_cooldown:{email}" (TTL 60s)
Set "otp:{email}" (TTL 5min)
```

### 2. Mail Sending Flow
Hệ thống xử lý gửi email bất đồng bộ (tùy chọn) để không chặn luồng chính.
```java
// Logic gửi OTP
public void sendOtp(String email) {
    String otp = generateOtp();
    redisService.set("otp:" + email, otp, 5, TimeUnit.MINUTES);
    
    EmailDTO emailDto = new EmailDTO();
    emailDto.setTo(email);
    emailDto.setSubject("Mã xác thực MovieBooking");
    emailDto.setContent("Mã của bạn là: " + otp);
    
    mailService.sendEmail(emailDto);
}
```

---

## 🔗 API ENDPOINTS

### 🔐 OTP APIs (`OtpController`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/otp/send` | Public | Gửi mã OTP về email (Rate limited) |
| `POST` | `/api/v1/otp/verify` | Public | Kiểm tra mã OTP người dùng nhập |

---

## 🛠️ CONFIGURATION

### application.yml:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls: enable

otp:
  resend-limit-seconds: 60
  ttl-minutes: 5

redis:
  host: localhost
  port: 6379
```

---

## 🧪 TESTING

1. **Test Rate Limit:** Bấm gửi OTP 2 lần liên tiếp. Lần 2 phải nhận được thông báo: "Vui lòng đợi 60 giây trước khi gửi lại".
2. **Test OTP Expiry:** Gửi OTP -> Đợi 6 phút -> Nhập OTP -> Hệ thống báo lỗi "Mã đã hết hạn".
3. **Test Mail Delivery:** Kiểm tra hộp thư đến (InBox) và thư rác (Spam).

---

## 📚 RELATED DOCS

- [Authentication Guide](03-AUTHENTICATION.md) - Cách tích hợp OTP vào luồng đăng ký.
- [Setup Guide](01-SETUP-GUIDE.md) - Cách lấy App Password cho Gmail.

---
