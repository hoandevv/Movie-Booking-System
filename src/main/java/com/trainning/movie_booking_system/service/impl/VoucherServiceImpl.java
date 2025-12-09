package com.trainning.movie_booking_system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherValidationResult;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.entity.VoucherUsage;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.VoucherMapper;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.VoucherRepository;
import com.trainning.movie_booking_system.repository.VoucherUsageRepository;
import com.trainning.movie_booking_system.service.IVoucherService;
import com.trainning.movie_booking_system.utils.enums.DiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final VoucherMapper voucherMapper;
    private final ObjectMapper objectMapper;

    // ===================== USER OPERATIONS =====================

    @Transactional(readOnly = true)
    @Override
    public VoucherValidationResult validateVoucher(ValidateVoucherRequest request, Long userId) {
        Voucher voucher = getActiveVoucherOrThrow(request.getVoucherCode());
        Booking booking = getBookingOrThrow(request.getBookingId());

        if (!isVoucherValidForUser(voucher, userId, request.getBookingAmount(), booking)) {
            return buildInvalidResult(voucher, request, userId, booking);
        }

        BigDecimal discountAmount = calculateDiscount(voucher, request.getBookingAmount(), booking);
        int userUsageCount = (int) voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), userId);

        return buildValidResult(voucher, request, discountAmount, userUsageCount);
    }

    @Override
    public VoucherUsageResponse applyVoucher(String voucherCode, Long bookingId, Long userId, BigDecimal discountAmount) {
        Voucher voucher = getVoucherByCodeOrThrow(voucherCode);
        Booking booking = getBookingOrThrow(bookingId);
        Account user = getAccountOrThrow(userId);

        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .booking(booking)
                .user(user)
                .originalAmount(booking.getTotalPrice())
                .discountAmount(discountAmount)
                .finalAmount(booking.getTotalPrice().subtract(discountAmount))
                .usedAt(LocalDateTime.now())
                .build();

        voucherUsageRepository.save(usage);
        incrementVoucherUsage(voucher);

        return voucherMapper.toUsageResponse(usage);
    }

    @Override
    public void refundVoucher(Long bookingId) {
        List<VoucherUsage> usages = voucherUsageRepository.findAllByBookingId(bookingId);
        for (VoucherUsage usage : usages) {
            decrementVoucherUsage(usage.getVoucher());
            voucherUsageRepository.delete(usage);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<VoucherResponse> getPublicVouchers(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Voucher> vouchers = voucherRepository.findActivePublicVouchers(VoucherStatus.ACTIVE, now, pageable);
        return vouchers.map(v -> {
            VoucherResponse response = voucherMapper.toResponse(v);
            response.setCanUse(v.getCurrentUsageCount() < v.getTotalUsageLimit());
            return response;
        });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<VoucherUsageResponse> getUserVoucherUsageHistory(Long userId, Pageable pageable) {
        return voucherUsageRepository.findByUserId(userId, pageable)
                .map(voucherMapper::toUsageResponse);
    }

    // ===================== ADMIN OPERATIONS =====================

    @Override
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        validateCreateRequest(request);
        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setCurrentUsageCount(0);

        if (request.getDiscountType() == DiscountType.BUY_X_GET_Y) {
            voucher.setDiscountValue(BigDecimal.ZERO);
            voucher.setMinOrderAmount(BigDecimal.ZERO);
            voucher.setMaxDiscountAmount(BigDecimal.ZERO);
        }
        return voucherMapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    public VoucherResponse updateVoucher(Long voucherId, UpdateVoucherRequest request) {
        Voucher voucher = getVoucherOrThrow(voucherId);
        validateUpdateRequest(request);
        voucherMapper.updateEntityFromRequest(request, voucher);
        return voucherMapper.toResponse(voucherRepository.save(voucher));
    }

    @Override
    public void deleteVoucher(Long voucherId) {
        Voucher voucher = getVoucherOrThrow(voucherId);
        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);
    }

    @Transactional(readOnly = true)
    @Override
    public VoucherResponse getVoucherById(Long voucherId) {
        Voucher voucher = getVoucherOrThrow(voucherId);
        return voucherMapper.toResponse(voucher);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable).map(voucherMapper::toResponse);
    }

    // ===================== SCHEDULED TASK =====================
    @Scheduled(cron = "0 0 0 * * ?") // hàng ngày lúc 00:00
    @Override
    public void deactivateExpiredVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> expiredVouchers = voucherRepository.findExpiredVouchers(VoucherStatus.ACTIVE, now);
        expiredVouchers.forEach(v -> v.setStatus(VoucherStatus.INACTIVE));
        voucherRepository.saveAll(expiredVouchers);
        log.info("Deactivated {} expired vouchers", expiredVouchers.size());
    }

    // ===================== PRIVATE HELPERS =====================

    private Voucher getVoucherOrThrow(Long voucherId) {
        return voucherRepository.findById(voucherId)
                .orElseThrow(() -> new NotFoundException("Voucher not found"));
    }

    private Voucher getVoucherByCodeOrThrow(String code) {
        return voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Voucher not found"));
    }

    private Voucher getActiveVoucherOrThrow(String code) {
        Voucher voucher = getVoucherByCodeOrThrow(code);
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new BadRequestException("Voucher is not active");
        }
        return voucher;
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private Account getAccountOrThrow(Long userId) {
        return accountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void incrementVoucherUsage(Voucher voucher) {
        voucher.setCurrentUsageCount(voucher.getCurrentUsageCount() + 1);
        voucherRepository.save(voucher);
    }

    private void decrementVoucherUsage(Voucher voucher) {
        voucher.setCurrentUsageCount(voucher.getCurrentUsageCount() - 1);
        voucherRepository.save(voucher);
    }

    private boolean isVoucherValidForUser(Voucher voucher, Long userId, BigDecimal bookingAmount, Booking booking) {
        if (LocalDateTime.now().isBefore(voucher.getValidFrom()) || LocalDateTime.now().isAfter(voucher.getValidUntil()))
            return false;
        if (voucher.getCurrentUsageCount() >= voucher.getTotalUsageLimit()) return false;
        if (voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), userId) >= voucher.getUsagePerUser()) return false;
        if (bookingAmount.compareTo(voucher.getMinOrderAmount()) < 0) return false;
        return isApplicableToBooking(voucher, booking);
    }

    private VoucherValidationResult buildValidResult(Voucher voucher, ValidateVoucherRequest request, BigDecimal discountAmount, int userUsageCount) {
        return VoucherValidationResult.builder()
                .isValid(true)
                .message("Voucher is valid")
                .voucherCode(voucher.getCode())
                .voucherName(voucher.getName())
                .originalAmount(request.getBookingAmount())
                .discountAmount(discountAmount)
                .finalAmount(request.getBookingAmount().subtract(discountAmount))
                .remainingUsage(voucher.getUsagePerUser() - userUsageCount)
                .validUntil(voucher.getValidUntil().toString())
                .build();
    }

    private VoucherValidationResult buildInvalidResult(Voucher voucher, ValidateVoucherRequest request, Long userId, Booking booking) {
        return VoucherValidationResult.builder()
                .isValid(false)
                .message("Voucher is not applicable")
                .voucherCode(request.getVoucherCode())
                .originalAmount(request.getBookingAmount())
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(request.getBookingAmount())
                .build();
    }

    private boolean isApplicableToBooking(Voucher voucher, Booking booking) {
        try {
            if (voucher.getApplicableMovieIds() != null && !voucher.getApplicableMovieIds().isEmpty()) {
                Set<Long> movieIds = objectMapper.readValue(voucher.getApplicableMovieIds(), new TypeReference<Set<Long>>() {});
                if (!movieIds.contains(booking.getShowtime().getMovie().getId())) return false;
            }
            if (voucher.getApplicableTheaterIds() != null && !voucher.getApplicableTheaterIds().isEmpty()) {
                Set<Long> theaterIds = objectMapper.readValue(voucher.getApplicableTheaterIds(), new TypeReference<Set<Long>>() {});
                if (!theaterIds.contains(booking.getShowtime().getScreen().getId())) return false;
            }
            if (voucher.getApplicableDaysOfWeek() != null && !voucher.getApplicableDaysOfWeek().isEmpty()) {
                Set<Integer> days = objectMapper.readValue(voucher.getApplicableDaysOfWeek(), new TypeReference<Set<Integer>>() {});
                int bookingDay = booking.getShowtime().getShowDate().getDayOfWeek().getValue();
                if (!days.contains(bookingDay)) return false;
            }
            if (voucher.getApplicableTimeSlots() != null && !voucher.getApplicableTimeSlots().isEmpty()) {
                List<String> slots = objectMapper.readValue(voucher.getApplicableTimeSlots(), new TypeReference<List<String>>() {});
                LocalTime bookingTime = booking.getShowtime().getStartTime();
                if (!isTimeInSlots(bookingTime, slots)) return false;
            }
        } catch (Exception e) {
            log.error("Failed to parse voucher JSON scope", e);
            return false;
        }
        return true;
    }

    private boolean isTimeInSlots(LocalTime time, List<String> timeSlots) {
        for (String slot : timeSlots) {
            String[] parts = slot.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            if (!time.isBefore(start) && !time.isAfter(end)) return true;
        }
        return false;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal bookingAmount, Booking booking) {
        if (voucher == null || booking == null || bookingAmount == null || bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Thông tin voucher hoặc booking không hợp lệ");
        }

        BigDecimal discount = BigDecimal.ZERO;
        switch (voucher.getDiscountType()) {
            case PERCENTAGE -> discount = bookingAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> discount = voucher.getDiscountValue();
            case BUY_X_GET_Y -> {
                int totalTickets = (booking.getBookingSeats() != null) ? booking.getBookingSeats().size() : 0;
                if (totalTickets < voucher.getBuyQuantity()) {
                    throw new BadRequestException("Không đủ số lượng vé để áp dụng voucher này");
                }
                int freeTickets = (totalTickets / voucher.getBuyQuantity()) * voucher.getGetQuantity();
                BigDecimal avgPrice = bookingAmount.divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP);
                discount = avgPrice.multiply(BigDecimal.valueOf(freeTickets));
            }
        }

        return discount.min(bookingAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateCreateRequest(CreateVoucherRequest request) {
        if (voucherRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Voucher code already exists");
        }
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Valid from must be before valid until");
        }
    }

    private void validateUpdateRequest(UpdateVoucherRequest request) {
        if (request.getValidFrom() != null && request.getValidUntil() != null &&
                request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Valid from must be before valid until");
        }
    }
}
