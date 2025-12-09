# TODO for Admin User Information Display and Email Verification

## 1. Update UserStatus Enum
- Modify `src/main/java/com/trainning/movie_booking_system/untils/enums/UserStatus.java` to include ACTIVE, INACTIVE, STAFF, BANNED

## 2. Create AdminUserResponse DTO
- Create `src/main/java/com/trainning/movie_booking_system/dto/response/Admin/AdminUserResponse.java` with fields:
  - email
  - username
  - phoneNumber
  - joinDate
  - bookingCount
  - totalSpending
  - status
  - emailVerified

## 3. Update UserService Interface
- Add method `AdminUserResponse getUserForAdmin(Long userId)` to `src/main/java/com/trainning/movie_booking_system/service/UserService.java`

## 4. Implement in UserServiceImpl
- Implement the method in `src/main/java/com/trainning/movie_booking_system/service/impl/UserServiceImpl.java`
  - Fetch user with account
  - Count bookings for account
  - Sum totalPrice for CONFIRMED bookings
  - Return AdminUserResponse

## 5. Update UserAdminController
- Add GET `/api/v1/admin/users/{id}` to return AdminUserResponse
- Add PUT `/api/v1/admin/users/{id}/verify-email` to set emailVerified = true

## 6. Followup Steps
- Test the endpoints
- Update database migration for UserStatus enum change if necessary
