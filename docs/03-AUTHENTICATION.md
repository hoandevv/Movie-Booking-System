# 🔐 Authentication - JWT Guide

> **Last Updated:** November 11, 2025  
> **Status:** ✅ Implemented

---

## 📋 OVERVIEW

Hệ thống sử dụng **JWT (JSON Web Token)** authentication với:
- ✅ Access Token (30 phút)
- ✅ Refresh Token (1 ngày, stored in Redis)
- ✅ Email verification (OTP)
- ✅ Password reset (OTP)
- ✅ Role-based access control (USER, ADMIN)

---

## 🔑 JWT TOKEN STRUCTURE

### Access Token Payload:

```json
{
  "accountId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "status": "ACTIVE",
  "emailVerified": true,
  "roles": ["USER"],
  "iat": 1699999999,
  "exp": 1700001799
}
```

### Refresh Token:

Stored in Redis:
```
Key: "refresh_token:{accountId}"
Value: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
TTL: 1 day (86400 seconds)
```

---

## 🚀 AUTHENTICATION FLOW

### 1. Registration Flow

```
POST /api/auth/register
   ↓
Validate input (username unique, email format)
   ↓
Hash password (BCrypt)
   ↓
Create account (status=ACTIVE, email_verified=false)
   ↓
Generate OTP (6 digits, TTL=5min)
   ↓
Store OTP in Redis: "otp:{email}" = "123456"
   ↓
Send email with OTP
   ↓
Return success (no tokens yet)
```

**Request:**
```json
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass@123",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "0912345678"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Registration successful. Please check email for OTP.",
  "data": null
}
```

---

### 2. Account Activation Flow

```
POST /api/auth/activate
   ↓
Verify OTP from Redis
   ↓
Update account: email_verified=true
   ↓
Delete OTP from Redis
   ↓
Return success
```

**Request:**
```json
POST /api/auth/activate
{
  "email": "john@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Account activated successfully. You can now login.",
  "data": null
}
```

**Errors:**
- `400 Bad Request` - Invalid or expired OTP
- `404 Not Found` - Email not found
- `409 Conflict` - Account already activated

---

### 3. Login Flow

```
POST /api/auth/login
   ↓
Find account by username/email
   ↓
Verify password (BCrypt compare)
   ↓
Check email_verified=true
   ↓
Check status=ACTIVE
   ↓
Generate access token (JWT, 30min)
   ↓
Generate refresh token (JWT, 1 day)
   ↓
Store refresh token in Redis
   ↓
Return both tokens
```

**Request:**
```json
POST /api/auth/login
{
  "username": "john_doe",
  "password": "SecurePass@123"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

**Errors:**
- `401 Unauthorized` - Invalid credentials
- `403 Forbidden` - Email not verified or account inactive

---

### 4. Refresh Token Flow

```
POST /api/auth/refresh-token
   ↓
Validate refresh token signature
   ↓
Check token in Redis (not revoked)
   ↓
Generate NEW access token
   ↓
Generate NEW refresh token
   ↓
Update Redis with new refresh token
   ↓
Return new tokens
```

**Request:**
```json
POST /api/auth/refresh-token
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

---

### 5. Logout Flow

```
POST /api/auth/logout
   ↓
Validate access token
   ↓
Delete refresh token from Redis
   ↓
(Optional) Add access token to blacklist
   ↓
Return success
```

**Request:**
```json
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "code": 200,
  "message": "Logout successful"
}
```

---

### 6. Forgot Password Flow

```
POST /api/auth/forgot-password
   ↓
Validate email exists
   ↓
Generate OTP (6 digits, TTL=5min)
   ↓
Store in Redis: "reset_otp:{email}" = "654321"
   ↓
Send email with OTP
   ↓
Return success
```

**Request:**
```json
POST /api/auth/forgot-password
{
  "email": "john@example.com"
}
```

---

### 7. Reset Password Flow

```
POST /api/auth/reset-password
   ↓
Verify OTP from Redis
   ↓
Hash new password (BCrypt)
   ↓
Update account password
   ↓
Delete OTP from Redis
   ↓
Invalidate all refresh tokens (security)
   ↓
Return success
```

**Request:**
```json
POST /api/auth/reset-password
{
  "email": "john@example.com",
  "otp": "654321",
  "newPassword": "NewSecurePass@456"
}
```

---

## 🔒 USING JWT IN REQUESTS

### Frontend Example (JavaScript):

```javascript
// 1. Login and store tokens
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({username: 'john_doe', password: 'pass123'})
});

const {data} = await loginResponse.json();
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);

// 2. Use access token for authenticated requests
const accessToken = localStorage.getItem('accessToken');
const response = await fetch('/api/bookings', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

// 3. Handle token expiration
if (response.status === 401) {
  // Access token expired, try refresh
  const refreshToken = localStorage.getItem('refreshToken');
  const refreshResponse = await fetch('/api/auth/refresh-token', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({refreshToken})
  });
  
  if (refreshResponse.ok) {
    const {data} = await refreshResponse.json();
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    // Retry original request
  } else {
    // Refresh token also expired, redirect to login
    window.location.href = '/login';
  }
}
```

---

## 🛡️ ROLE-BASED ACCESS CONTROL

### Roles in System:

```java
public enum RoleType {
    USER,                  // Regular user
    ADMIN,                 // Full access
    THEATER_MANAGEMENT     // Manage theaters/screens
}
```

### Protected Endpoints Examples:

```java
// Admin only
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/movies")
public ResponseEntity<MovieDTO> createMovie(@RequestBody MovieRequest request) {
    // Only ADMIN can create movies
}

// User or Admin
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@PostMapping("/api/bookings")
public ResponseEntity<BookingDTO> createBooking(@RequestBody BookingRequest request) {
    // Both USER and ADMIN can book
}

// Any authenticated user
@GetMapping("/api/auth/me")
public ResponseEntity<UserDTO> getCurrentUser() {
    // Any logged-in user can access
}
```

---

## 🔐 SECURITY FEATURES

### 1. Password Security

```java
// BCrypt with salt (automatically handled)
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

// Verify
boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
```

### 2. JWT Signature

```java
// Sign token with HMAC SHA-256
String jwt = Jwts.builder()
    .setSubject(username)
    .claim("accountId", accountId)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 1800000))
    .signWith(SignatureAlgorithm.HS256, secret)
    .compact();
```

### 3. OTP Rate Limiting

```yaml
# application.yml
otp:
  max-attempts-per-day: 5
  cooldown-seconds: 60
```

**Redis Implementation:**

```
Key: "otp_attempts:{email}"
Value: count (incremented on each request)
TTL: 24 hours

Key: "otp_cooldown:{email}"
Value: timestamp
TTL: 60 seconds
```

---

## 🧪 TESTING

### Postman Collection:

Import `Movie_Booking_System_V1_Collection.postman_collection.json`

**Test sequence:**

```
1. Register → Check email → Activate
2. Login → Save accessToken & refreshToken to environment
3. Test protected endpoint → Should succeed
4. Wait 31 minutes (or change token expiry to 1 min)
5. Test again → Should fail with 401
6. Refresh token → Get new accessToken
7. Test again → Should succeed
8. Logout
9. Test again → Should fail with 401
```

---

## 📊 CONFIGURATION

### application.yml:

```yaml
jwt:
  expiryMinutes: 30          # Access token expiry
  expiryDay: 1               # Refresh token expiry (days)
  accessKey: YOUR_SECRET_KEY_HERE  # 512-bit secret

otp:
  ttl-minutes: 5
  max-attempts-per-day: 5
  cooldown-seconds: 60

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
```

---

## 🐛 TROUBLESHOOTING

### Issue: "Invalid JWT signature"

**Cause:** Secret key mismatch or token tampered

**Solution:**
- Verify `jwt.accessKey` in application.yml
- Don't modify token manually
- Check token wasn't copied with extra spaces

### Issue: "Refresh token not found"

**Cause:** Redis connection issue or token expired

**Solution:**
```bash
# Check Redis
redis-cli
> KEYS refresh_token:*
> TTL refresh_token:1  # Should be > 0
```

### Issue: OTP not received

**Solution:**
- Check spam folder
- Verify Gmail App Password (not regular password)
- Check logs: `tail -f logs/spring-boot-logger.log | grep "Email sent"`

---

## 🎯 BEST PRACTICES

1. **Never expose JWT secret** in client-side code
2. **Always use HTTPS** in production
3. **Store tokens in httpOnly cookies** (more secure than localStorage)
4. **Implement token refresh** before expiry (not after)
5. **Invalidate all tokens** on password change
6. **Log all authentication events** for audit

---

## 📚 RELATED DOCS

- [API Documentation](02-API-DOCUMENTATION.md) - All auth endpoints
- [Testing Guide](07-TESTING-GUIDE.md) - Auth test scenarios
- [Setup Guide](01-SETUP-GUIDE.md) - Email configuration

---

**🔒 Security is critical - Review this carefully!**
