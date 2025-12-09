package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Voucher operations
 */
public interface IVoucherService {

    /**
     * Validate if a voucher can be applied to a booking
     * Performs 8-step validation:
     * 1. Check voucher exists by code
     * 2. Check voucher is ACTIVE
     * 3. Check current date is within valid period
     * 4. Check total usage limit not exceeded
     * 5. Check user usage limit not exceeded
     * 6. Check minimum order amount requirement
     * 7. Check applicable scope (movies, theaters, days, time slots)
     * 8. Calculate discount based on discount type
     *
     * @param request Validation request with voucher code, booking ID, and amount
     * @param userId  Current user ID (for usage limit check)
     * @return Validation result with discount calculation
     */
    VoucherValidationResult validateVoucher(ValidateVoucherRequest request, Long userId);

    /**
     * Apply a voucher to a booking
     * Creates a VoucherUsage record after successful validation
     *
     * @param voucherCode   Voucher code to apply
     * @param bookingId     Booking ID
     * @param userId        User ID
     * @param discountAmount Calculated discount amount
     * @return VoucherUsageResponse with usage details
     */
    VoucherUsageResponse applyVoucher(String voucherCode, Long bookingId, Long userId, java.math.BigDecimal discountAmount);

    /**
     * Refund voucher usage when booking is cancelled
     * Deletes VoucherUsage record and decrements voucher usage count
     *
     * @param bookingId Booking ID to refund
     */
    void refundVoucher(Long bookingId);

    /**
     * Get all public vouchers that are currently valid
     * Used for displaying available vouchers to users
     *
     * @param pageable Pagination parameters
     * @return Page of public active vouchers
     */
    Page<VoucherResponse> getPublicVouchers(Pageable pageable);

    /**
     * Get voucher usage history for a user
     *
     * @param userId   User ID
     * @param pageable Pagination parameters
     * @return Page of user's voucher usage history
     */
    Page<VoucherUsageResponse> getUserVoucherUsageHistory(Long userId, Pageable pageable);

    /**
     * Create a new voucher (ADMIN only)
     *
     * @param request Voucher creation request
     * @return Created voucher response
     */
    VoucherResponse createVoucher(CreateVoucherRequest request);

    /**
     * Update an existing voucher (ADMIN only)
     *
     * @param voucherId Voucher ID to update
     * @param request   Update request
     * @return Updated voucher response
     */
    VoucherResponse updateVoucher(Long voucherId, UpdateVoucherRequest request);

    /**
     * Delete a voucher (ADMIN only)
     * Soft delete by setting status to INACTIVE
     *
     * @param voucherId Voucher ID to delete
     */
    void deleteVoucher(Long voucherId);

    /**
     * Get voucher by ID (ADMIN only)
     *
     * @param voucherId Voucher ID
     * @return Voucher response
     */
    VoucherResponse getVoucherById(Long voucherId);

    /**
     * Get all vouchers with filters (ADMIN only)
     *
     * @param pageable Pagination parameters
     * @return Page of all vouchers
     */
    Page<VoucherResponse> getAllVouchers(Pageable pageable);

    /**
     * Deactivate expired vouchers (scheduled task)
     * Runs daily to update ACTIVE vouchers that passed validUntil date
     */
    void deactivateExpiredVouchers();
}
