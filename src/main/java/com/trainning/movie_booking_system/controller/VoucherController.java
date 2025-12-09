package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherValidationResult;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.service.IVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Voucher operations
 * Handles CRUD, validation, and usage history
 */
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Voucher", description = "Voucher management APIs")
public class VoucherController {

    private final IVoucherService voucherService;

    // =========================
    // USER ENDPOINTS
    // =========================

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Validate voucher", description = "Validate if a voucher can be applied and calculate discount")
    public ResponseEntity<VoucherValidationResult> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request,
            @AuthenticationPrincipal CustomAccountDetails accountDetails
    ) {
        Long userId = accountDetails.getAccount().getId();
        log.info("User {} validating voucher: {}", userId, request.getVoucherCode());
        VoucherValidationResult result = voucherService.validateVoucher(request, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public vouchers", description = "Get all active public vouchers available for use")
    public ResponseEntity<Page<VoucherResponse>> getPublicVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "validUntil") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        log.info("Fetching public vouchers - page: {}, size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VoucherResponse> vouchers = voucherService.getPublicVouchers(pageable);
        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/my-usage")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my voucher usage history", description = "Retrieve the current user's voucher usage history")
    public ResponseEntity<Page<VoucherUsageResponse>> getMyVoucherUsageHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomAccountDetails accountDetails
    ) {
        Long userId = accountDetails.getAccount().getId();
        log.info("Fetching voucher usage history for user {}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("usedAt").descending());
        Page<VoucherUsageResponse> usageHistory = voucherService.getUserVoucherUsageHistory(userId, pageable);
        return ResponseEntity.ok(usageHistory);
    }

    // =========================
    // ADMIN ENDPOINTS
    // =========================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create voucher (ADMIN)", description = "Create a new voucher (Admin only)")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        log.info("Admin creating voucher: {}", request.getCode());
        VoucherResponse voucher = voucherService.createVoucher(request);
        return ResponseEntity.ok(voucher);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update voucher (ADMIN)", description = "Update an existing voucher (Admin only)")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherRequest request
    ) {
        log.info("Admin updating voucher ID: {}", id);
        VoucherResponse voucher = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(voucher);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete voucher (ADMIN)", description = "Soft delete a voucher by setting status to INACTIVE")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        log.info("Admin deleting voucher ID: {}", id);
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get voucher by ID (ADMIN)", description = "Retrieve voucher details by ID")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        log.info("Fetching voucher ID: {}", id);
        VoucherResponse voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(voucher);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all vouchers (ADMIN)", description = "Get all vouchers with pagination")
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        log.info("Fetching all vouchers - page: {}, size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VoucherResponse> vouchers = voucherService.getAllVouchers(pageable);
        return ResponseEntity.ok(vouchers);
    }
}