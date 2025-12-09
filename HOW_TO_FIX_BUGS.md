# 🔧 HƯỚNG DẪN FIX BUGS - Từng Bước Chi Tiết

> **Mục đích:** Hướng dẫn fix từng bug theo đúng quy trình development  
> **Nguyên tắc:** Một bug một branch → Test → Merge

---

## 📋 QUY TRÌNH FIX 1 BUG

```
1. Tạo branch mới
   ↓
2. Implement fix
   ↓
3. Write/update tests
   ↓
4. Run tests locally
   ↓
5. Commit với message rõ ràng
   ↓
6. Push & create PR
   ↓
7. Code review
   ↓
8. Merge vào develop/master
```

---

## 🔴 BUG #1: Showtime Validation (EASIEST - START HERE!)

**Time:** 30 phút  
**Difficulty:** ⭐ Easy  
**Priority:** 🔴 CRITICAL

### Step 1: Tạo Branch

```bash
git checkout feature/payment-voucher-integration
git pull origin feature/payment-voucher-integration
git checkout -b bugfix/showtime-validation
```

### Step 2: Implement Fix

**File:** `src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java`

**Tìm method `create()`:**
```java
@Override
public BookingDTO create(BookingRequest request) {
    // 1. Get showtime
    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
    
    // ✅ ADD: Validate showtime
    validateShowtime(showtime);
    
    // ... rest of code
}
```

**Thêm method mới (cuối class):**
```java
/**
 * Validate showtime chưa bắt đầu và còn thời gian đặt vé
 * @param showtime Showtime cần validate
 * @throws BadRequestException nếu không hợp lệ
 */
private void validateShowtime(Showtime showtime) {
    LocalDateTime now = LocalDateTime.now();
    
    // Combine show_date + start_time thành LocalDateTime
    LocalDateTime showtimeStart = LocalDateTime.of(
        showtime.getShowDate(),
        showtime.getStartTime()
    );
    
    // Rule 1: Không được book suất chiếu đã qua
    if (showtimeStart.isBefore(now)) {
        log.warn("Attempt to book past showtime: {}", showtime.getId());
        throw new BadRequestException(
            String.format("Cannot book for past showtime. Showtime was at %s", 
                showtimeStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        );
    }
    
    // Rule 2: Đóng booking 15 phút trước giờ chiếu
    LocalDateTime cutoffTime = showtimeStart.minusMinutes(15);
    if (now.isAfter(cutoffTime)) {
        log.warn("Attempt to book within cutoff time: {}", showtime.getId());
        throw new BadRequestException(
            "Booking closes 15 minutes before showtime. Cutoff time was " 
            + cutoffTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }
    
    log.debug("Showtime validation passed for showtime ID: {}", showtime.getId());
}
```

**Import cần thêm:**
```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

### Step 3: Write Tests

**File:** `src/test/java/com/trainning/movie_booking_system/service/BookingServiceTest.java`

```java
@Test
@DisplayName("Should throw exception when booking past showtime")
void testBookingPastShowtime() {
    // Arrange
    Showtime pastShowtime = Showtime.builder()
        .id(1L)
        .showDate(LocalDate.now().minusDays(1))
        .startTime(LocalTime.of(20, 0))
        .build();
    
    when(showtimeRepository.findById(1L))
        .thenReturn(Optional.of(pastShowtime));
    
    BookingRequest request = new BookingRequest();
    request.setShowtimeId(1L);
    request.setSeatIds(List.of(1L, 2L));
    
    // Act & Assert
    assertThrows(BadRequestException.class, () -> {
        bookingService.create(request);
    });
}

@Test
@DisplayName("Should throw exception when booking within cutoff time")
void testBookingWithinCutoffTime() {
    // Arrange: Showtime in 10 minutes (within 15-minute cutoff)
    LocalDateTime tenMinutesLater = LocalDateTime.now().plusMinutes(10);
    
    Showtime nearShowtime = Showtime.builder()
        .id(1L)
        .showDate(tenMinutesLater.toLocalDate())
        .startTime(tenMinutesLater.toLocalTime())
        .build();
    
    when(showtimeRepository.findById(1L))
        .thenReturn(Optional.of(nearShowtime));
    
    BookingRequest request = new BookingRequest();
    request.setShowtimeId(1L);
    request.setSeatIds(List.of(1L, 2L));
    
    // Act & Assert
    assertThrows(BadRequestException.class, () -> {
        bookingService.create(request);
    });
}

@Test
@DisplayName("Should allow booking for future showtime")
void testBookingFutureShowtime() {
    // Arrange: Showtime tomorrow
    LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
    
    Showtime futureShowtime = Showtime.builder()
        .id(1L)
        .showDate(tomorrow.toLocalDate())
        .startTime(tomorrow.toLocalTime())
        .screen(screen)
        .price(BigDecimal.valueOf(150000))
        .build();
    
    when(showtimeRepository.findById(1L))
        .thenReturn(Optional.of(futureShowtime));
    
    // Mock other dependencies...
    
    // Act
    BookingDTO result = bookingService.create(request);
    
    // Assert
    assertNotNull(result);
}
```

### Step 4: Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Verify
# ✅ All tests should pass
```

### Step 5: Manual Testing (Postman)

**Test Case 1: Book past showtime (Should fail)**
```http
POST http://localhost:8080/api/bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "showtimeId": 1,  // Showtime đã qua
  "seatIds": [1, 2]
}

Expected Response: 400 Bad Request
{
  "success": false,
  "message": "Cannot book for past showtime. Showtime was at 2024-11-10 20:00"
}
```

**Test Case 2: Book within 15 minutes (Should fail)**
```http
POST http://localhost:8080/api/bookings
{
  "showtimeId": 2,  // Showtime bắt đầu trong 10 phút
  "seatIds": [3, 4]
}

Expected: 400 Bad Request
"Booking closes 15 minutes before showtime..."
```

**Test Case 3: Book future showtime (Should succeed)**
```http
POST http://localhost:8080/api/bookings
{
  "showtimeId": 3,  // Showtime ngày mai
  "seatIds": [5, 6]
}

Expected: 201 Created
```

### Step 6: Commit & Push

```bash
# Check changes
git status
git diff

# Add files
git add src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java
git add src/test/java/com/trainning/movie_booking_system/service/BookingServiceTest.java

# Commit với message chuẩn
git commit -m "fix: Add showtime validation to prevent booking past showtimes

WHAT CHANGED:
- Added validateShowtime() method in BookingServiceImpl
- Validates showtime hasn't started
- Validates booking within cutoff time (15 minutes before)

WHY:
- Users could book tickets for past showtimes
- No validation for showtime start time
- Business rule: booking closes 15 minutes before showtime

TESTING:
- Added unit tests for past showtime validation
- Added unit tests for cutoff time validation
- All existing tests still pass

Fixes: BUG #1 - Showtime Validation
Related: docs/BUGS_TO_FIX.md"

# Push
git push origin bugfix/showtime-validation
```

### Step 7: Create Pull Request

**PR Title:**
```
fix: Add showtime validation to prevent booking past showtimes
```

**PR Description:**
```markdown
## What
Add validation to prevent booking for past showtimes and within cutoff time.

## Why
- Users could book tickets for showtimes that already started
- No business rule enforcement for booking cutoff time
- Fixes BUG #1 from BUGS_TO_FIX.md

## Changes
- Added `validateShowtime()` method in `BookingServiceImpl`
- Check showtime hasn't started
- Check booking within 15-minute cutoff window
- Added comprehensive unit tests

## Testing
- ✅ Unit tests pass
- ✅ Integration tests pass  
- ✅ Manual testing with Postman

## Screenshots
(Attach Postman test results)

## Related Issues
Closes BUG #1 in BUGS_TO_FIX.md
```

---

## 🔴 BUG #2: BookingSeat thiếu rowLabel

**Time:** 1 giờ  
**Difficulty:** ⭐⭐ Medium  
**Priority:** 🔴 CRITICAL

### Step 1: Tạo Branch

```bash
git checkout feature/payment-voucher-integration
git checkout -b bugfix/booking-seat-row-label
```

### Step 2: Database Migration

**File:** Create `src/main/resources/db/migration/V2__add_booking_seat_denormalized_fields.sql`

```sql
-- Add denormalized seat info to booking_seats table
ALTER TABLE booking_seats 
ADD COLUMN seat_number INT NOT NULL DEFAULT 0 AFTER seat_id,
ADD COLUMN row_label VARCHAR(10) NOT NULL DEFAULT '' AFTER seat_number,
ADD COLUMN seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' AFTER row_label;

-- Migrate existing data from seats table
UPDATE booking_seats bs
INNER JOIN seats s ON bs.seat_id = s.id
SET 
    bs.seat_number = s.seat_number,
    bs.row_label = s.row_label,
    bs.seat_type = s.seat_type;

-- Remove default values (they were only for migration)
ALTER TABLE booking_seats 
ALTER COLUMN seat_number DROP DEFAULT,
ALTER COLUMN row_label DROP DEFAULT,
ALTER COLUMN seat_type DROP DEFAULT;

-- Add index for better query performance
CREATE INDEX idx_booking_seat_info ON booking_seats(row_label, seat_number);
```

### Step 3: Update Entity

**File:** `src/main/java/com/trainning/movie_booking_system/entity/BookingSeat.java`

```java
@Entity
@Table(
    name = "booking_seats",
    indexes = {
        @Index(name = "idx_booking", columnList = "booking_id"),
        @Index(name = "idx_booking_seat", columnList = "seat_id"),
        @Index(name = "idx_booking_seat_info", columnList = "row_label, seat_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    // ✅ ADD: Denormalized seat information
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;
    
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;
}
```

### Step 4: Update DTO

**File:** `src/main/java/com/trainning/movie_booking_system/dto/response/BookingSeatDTO.java`

```java
@Data
@Builder
public class BookingSeatDTO {
    private Long id;
    private Long seatId;
    private BigDecimal price;
    
    // ✅ ADD: Display information
    private int seatNumber;
    private String rowLabel;
    private SeatType seatType;
    
    /**
     * Computed field: "A1", "B2", "C3"
     */
    public String getSeatLabel() {
        return rowLabel + seatNumber;
    }
}
```

### Step 5: Update Service

**File:** `BookingServiceImpl.java`

```java
private BookingSeat createBookingSeat(Booking booking, Seat seat, BigDecimal price) {
    return BookingSeat.builder()
        .booking(booking)
        .seat(seat)
        .price(price)
        // ✅ ADD: Copy seat display info
        .seatNumber(seat.getSeatNumber())
        .rowLabel(seat.getRowLabel())
        .seatType(seat.getSeatType())
        .build();
}
```

### Step 6: Update Mapper

**File:** `BookingMapper.java`

```java
@Mapper(componentModel = "spring")
public interface BookingMapper {
    
    @Mapping(target = "seatLabel", expression = "java(bookingSeat.getRowLabel() + bookingSeat.getSeatNumber())")
    BookingSeatDTO toBookingSeatDTO(BookingSeat bookingSeat);
    
    // ... other mappings
}
```

### Step 7: Run Migration & Test

```bash
# Run migration
mvn flyway:migrate

# Or if using spring.jpa.hibernate.ddl-auto=update
mvn spring-boot:run

# Verify database
mysql -u root -p movie_booking
> DESC booking_seats;
# Should see: seat_number, row_label, seat_type columns

# Run tests
mvn test
```

### Step 8: Manual Testing

```http
POST /api/bookings
{
  "showtimeId": 1,
  "seatIds": [1, 2, 3]
}

Expected Response:
{
  "id": 100,
  "seats": [
    {
      "id": 1,
      "seatId": 1,
      "price": 150000,
      "seatNumber": 1,
      "rowLabel": "A",
      "seatType": "STANDARD",
      "seatLabel": "A1"  // ✅ Frontend có thể hiển thị!
    },
    {
      "id": 2,
      "seatId": 2,
      "price": 195000,
      "seatNumber": 2,
      "rowLabel": "A",
      "seatType": "VIP",
      "seatLabel": "A2"
    }
  ]
}
```

### Step 9: Commit & Push

```bash
git add .
git commit -m "fix: Add denormalized seat info to booking_seats table

WHAT CHANGED:
- Added seat_number, row_label, seat_type to BookingSeat entity
- Created database migration script
- Updated BookingSeatDTO with display fields
- Added seatLabel computed property

WHY:
- Frontend couldn't display seat information without extra queries
- N+1 query problem when loading booking details
- Improved performance (no join needed)

DATABASE CHANGES:
- ALTER TABLE booking_seats ADD COLUMN seat_number, row_label, seat_type
- Migrated data from seats table
- Added index on (row_label, seat_number)

TESTING:
- Database migration successful
- Unit tests updated and passing
- API returns seat labels correctly

Fixes: BUG #2 - BookingSeat missing rowLabel"

git push origin bugfix/booking-seat-row-label
```

---

## 🔴 BUG #3: Booking Expiration

**Time:** 3 giờ  
**Difficulty:** ⭐⭐⭐ Hard  
**Priority:** 🔴 CRITICAL

### Hướng dẫn chi tiết trong file riêng...

*(Tương tự format như trên cho các bugs còn lại)*

---

## 📊 PROGRESS TRACKING

| Bug | Branch | Status | PR | Notes |
|-----|--------|--------|-----|-------|
| #1 Showtime validation | `bugfix/showtime-validation` | ⚠️ TODO | - | Start here! |
| #2 BookingSeat rowLabel | `bugfix/booking-seat-row-label` | ⚠️ TODO | - | Needs migration |
| #3 Booking expiration | `bugfix/booking-expiration` | ⚠️ TODO | - | Complex |
| #4 Payment idempotency | `bugfix/payment-idempotency` | ⚠️ TODO | - | Redis lock |
| #5 Payment IPN | `bugfix/payment-ipn-webhook` | ⚠️ TODO | - | New endpoint |
| #6 VNPay integration | `feature/vnpay-integration` | ⚠️ TODO | - | 2-3 days |
| #7 Cleanup showtimes | `bugfix/cleanup-completed-showtimes` | ⚠️ TODO | - | New cron |

---

## 📝 COMMIT MESSAGE FORMAT

```
<type>: <subject>

WHAT CHANGED:
- List of changes

WHY:
- Reason for changes

TESTING:
- How it was tested

Fixes: BUG #X
```

**Types:**
- `fix:` - Bug fix
- `feat:` - New feature
- `refactor:` - Code refactoring
- `test:` - Adding tests
- `docs:` - Documentation
- `chore:` - Maintenance

---

## 🎯 NEXT STEPS

1. **BẮT ĐẦU:** Đọc hướng dẫn BUG #1 ở trên
2. **LÀM THEO:** Từng bước một, không skip
3. **HỎI:** Nếu gặp vấn đề không rõ
4. **COMMIT:** Sau khi test pass
5. **NEXT:** Chuyển sang BUG #2

**Sẵn sàng bắt đầu với BUG #1?** 🚀
