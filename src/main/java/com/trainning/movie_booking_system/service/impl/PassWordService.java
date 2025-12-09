package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.ForgotPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ResetPasswordRequest;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.utils.enums.OtpType;
import com.trainning.movie_booking_system.utils.enums.UserStatus;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.service.OtpService;
import com.trainning.movie_booking_system.service.RedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassWordService {

    private final AccountRepository accountRepository;
    private final OtpService otpService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    /**
     * FORGOT PASSWORD:
     * . Find account
     * . Check account ACTIVE
     * . Check email VERIFIED
     * . Send OTP
     * . Response: "OTP sent to email"
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("[PASSWORD] Forgot password for email: {}", request.getEmail());

        // 1. Find account
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[PASSWORD] Account not found for email: {}", request.getEmail());
                    return new BadRequestException("Email not found in system");
                });

        // 2. Check account ACTIVE
        if (!UserStatus.ACTIVE.equals(account.getStatus())) {
            log.warn("[PASSWORD] Inactive account trying forgot password: {}", account.getUsername());
            throw new BadRequestException("Account is not active");
        }

        // 3. Check email verified (nếu bạn có trường isEmailVerified trong Account)
        if (!account.isEmailVerified()) {
            log.warn("[PASSWORD] Email not verified for: {}", account.getEmail());
            throw new BadRequestException("Email has not been verified");
        }

        // 4. Send OTP
        try {
            otpService.sendOtp(account.getEmail(), OtpType.FORGOT_PASSWORD);
            log.info("[PASSWORD] Forgot password OTP sent to email: {}", account.getEmail());
        } catch (Exception e) {
            log.error("[PASSWORD] Failed to send forgot password OTP", e);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
    }

    /**
     * RESET PASSWORD:
     *. Verify OTP
     *. Find account
     *. Update password (encode)
     *. Delete OTP ← Cleanup
     *. Delete refresh tokens ← Force re-login
     *. Response: "Password reset success"
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("[PASSWORD] Reset password request for email: {}", request.getEmail());

        // 1. Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("[PASSWORD] Password mismatch for email: {}", request.getEmail());
            throw new BadRequestException("Passwords do not match");
        }

        // 2. Verify OTP
        boolean isValidOtp = otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                OtpType.FORGOT_PASSWORD
        );
        if (!isValidOtp) {
            log.warn("[PASSWORD] Invalid OTP for email: {}", request.getEmail());
            throw new BadRequestException("Invalid or expired OTP");
        }
        // 3. Find account
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[PASSWORD] Account not found during password reset: {}", request.getEmail());
                    return new BadRequestException("Account not found");
                });

        // 4. Encode and update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(encodedPassword);
        accountRepository.save(account);
        log.info("[PASSWORD] Password updated successfully for account: {}", account.getUsername());

        // 5. Delete OTP
        try {
            otpService.deleteOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
            log.debug("[PASSWORD] OTP deleted for email: {}", request.getEmail());
        } catch (Exception e) {
            log.warn("[PASSWORD] Failed to delete OTP, but password was reset successfully", e);
        }

        // 6. Invalidate refresh tokens
        try {
            String redisKey = buildRedisKey(account.getUsername());
            redisService.delete(redisKey);
            log.debug("[PASSWORD] Refresh tokens invalidated for user: {}", account.getUsername());
        } catch (Exception e) {
            log.warn("[PASSWORD] Failed to invalidate refresh tokens, but password was reset successfully", e);
        }

        log.info("[PASSWORD] Password reset completed successfully for user: {}", account.getUsername());
    }

    /**
     * Helper method to build Redis key for refresh token
     */
    private String buildRedisKey(String username) {
        return "auth:refreshToken:" + username;
    }
}
