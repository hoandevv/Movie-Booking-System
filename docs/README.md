# 📚 Documentation Index

> **Dự án:** Movie Booking System  
> **Tech Stack:** Spring Boot 3, MySQL, Redis, VNPay  
> **Last Updated:** November 11, 2025

---

## 📖 TABLE OF CONTENTS

| #  | Document | Description | Status |
|----|----------|-------------|--------|
| 1  | [Setup Guide](01-SETUP-GUIDE.md) | Cài đặt & chạy dự án | ✅ Ready |
| 2  | [API Documentation](02-API-DOCUMENTATION.md) | REST API endpoints | ✅ Ready |
| 3  | [Authentication](03-AUTHENTICATION.md) | JWT authentication flow | ✅ Ready |
| 4  | [Booking Flow](04-BOOKING-FLOW.md) | Seat booking with concurrency control | ✅ **FIXED** |
| 5  | [Payment Flow](05-PAYMENT-FLOW.md) | VNPay payment integration | ✅ **FIXED** |
| 6  | [Voucher Feature](06-VOUCHER-FEATURE.md) | Discount voucher system | ⚠️ **TODO** |
| 7  | [Testing Guide](07-TESTING-GUIDE.md) | Unit & Integration tests | ✅ Ready |

---

## 🚀 QUICK START

### For Developers:
1. Read **Setup Guide** to run the project
2. Read **API Documentation** to understand endpoints
3. Import Postman collection from `docs/postman/`

### For Code Review:
1. Read **Booking Flow** - Critical concurrency handling
2. Read **Payment Flow** - VNPay integration with idempotency
3. Check **Testing Guide** for test coverage

---

## 📝 NOTES

- ✅ **Green status** = Implemented & documented
- ⚠️ **Yellow status** = Planned but not yet implemented
- 🔴 **Red status** = Deprecated or archived

---

## 🗂️ ARCHIVED DOCUMENTS

Old/redundant documents have been moved to `/.archive/` folder.

See [Archive Index](../.archive/README.md) for details.
