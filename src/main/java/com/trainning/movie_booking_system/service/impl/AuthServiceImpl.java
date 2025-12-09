package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.*;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.InternalServerErrorException;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.security.JwtProvider;
import com.trainning.movie_booking_system.service.*;
import com.trainning.movie_booking_system.utils.enums.OtpType;
import com.trainning.movie_booking_system.utils.enums.RoleType;
import com.trainning.movie_booking_system.utils.enums.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

import static com.trainning.movie_booking_system.mapper.AuthMapper.toResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final OtpService otpService;

    // ================= REGISTER ================= //
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Starting registration for username: {}", request.getUsername());

        validateField(request);

        Account account = buildAccount(request);

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        AccountHasRole accountRole = buildAccountRole(account, userRole);
        account.getAccountRoles().add(accountRole);

        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        User user = buildProfileUser(request, savedAccount);
        userRepository.save(user);
        log.info("User profile created successfully for account: {}", savedAccount.getUsername());

        otpService.sendOtp(request.getEmail(), OtpType.REGISTER);
        log.info("Registration successful for {}, awaiting OTP verification", request.getEmail());
    }

    // ================= ACTIVATE ACCOUNT ================= //
    @Override
    @Transactional
    public void activateAccount(VerifyOtpRequest request) {
        log.info("Activating account for email: {}", request.getEmail());

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.REGISTER);
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        account.setEmailVerified(true);
        accountRepository.save(account);
        otpService.deleteOtp(request.getEmail(), OtpType.REGISTER);

        log.info("Account {} activated successfully", account.getUsername());
    }

    // ================= LOGIN ================= //
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Starting login for username: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomAccountDetails accountDetails = (CustomAccountDetails) authentication.getPrincipal();
            Account account = accountDetails.account();

            // Tạo token
            String accessToken = jwtProvider.generateToken(account);
            String refreshToken = jwtProvider.generateRefreshToken(account);

            // Lưu refresh token vào Redis
            String key = buildRedisKey(account.getUsername());
            long ttl = (jwtProvider.getExpiration(refreshToken).getTime() - System.currentTimeMillis()) / 1000;
            redisService.set(key, refreshToken, ttl, TimeUnit.SECONDS);

            log.info("Login successful for username: {}", account.getUsername());
            return toResponse(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid username or password");
        } catch (DisabledException e) {
            throw new BadRequestException("Account is inactive or email not verified");
        } catch (LockedException e) {
            throw new BadRequestException("Account is locked");
        } catch (AuthenticationException e) {
            throw new BadRequestException("Invalid username or password");
        } catch (Exception e) {
            log.error("Unexpected error during login for {}: {}", request.getUsername(), e.getMessage(), e);
            throw new InternalServerErrorException("Unexpected error while logging in");
        }
    }

    // ================= REFRESH TOKEN ================= //
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Starting refresh token process");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        String cleanToken = cleanToken(refreshToken);
        if (!jwtProvider.validateToken(cleanToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(cleanToken);
            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            verifyStoredRefreshToken(username, cleanToken);

            if (!jwtProvider.isTokenValidForAccount(cleanToken, account)) {
                throw new BadRequestException("Token does not match user");
            }

            String newAccessToken = jwtProvider.generateToken(account);
            log.info("Access token refreshed successfully for user: {}", username);
            return toResponse(newAccessToken, cleanToken);

        } catch (ExpiredJwtException e) {
            throw new BadRequestException("Refresh token expired");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new BadRequestException("Failed to refresh token");
        }
    }

    // ================= LOGOUT ================= //
    @Override
    public void logout(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new BadRequestException("Refresh token is required");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(refreshToken);
            if (StringUtils.isBlank(username)) {
                throw new BadRequestException("Failed to extract username from token");
            }

            String redisKey = buildRedisKey(username);
            redisService.delete(redisKey);

            log.info("Logout successful for user: {}", username);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to logout", e);
            throw new BadRequestException("Logout operation failed");
        }
    }

    // ================= FORGOT PASSWORD ================= //
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password for email: {}", request.getEmail());

        if (StringUtils.isBlank(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email not found in system"));

        switch (account.getStatus()) {
            case ACTIVE -> { /* ok */ }
            case INACTIVE -> throw new BadRequestException("Account is inactive. Please contact support.");
            case LOCKED -> throw new BadRequestException("Account is locked. Please contact support.");
            default -> throw new InternalServerErrorException("Unknown account status");
        }

        try {
            otpService.sendOtp(account.getEmail(), OtpType.FORGOT_PASSWORD);
            log.info("Forgot password OTP sent to email: {}", account.getEmail());
        } catch (Exception e) {
            log.error("Failed to send forgot password OTP", e);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
    }

    // ================= RESET PASSWORD ================= //
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());

        if (StringUtils.isBlank(request.getEmail())) throw new BadRequestException("Email is required");
        if (StringUtils.isBlank(request.getOtp())) throw new BadRequestException("OTP is required");
        if (StringUtils.isBlank(request.getNewPassword())) throw new BadRequestException("New password is required");
        if (StringUtils.isBlank(request.getConfirmPassword())) throw new BadRequestException("Confirm password is required");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        boolean isValidOtp = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.FORGOT_PASSWORD);
        if (!isValidOtp) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(encodedPassword);
        accountRepository.save(account);
        log.info("Password updated successfully for account: {}", account.getId());

        try {
            otpService.deleteOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
        } catch (Exception e) {
            log.warn("Failed to delete OTP, but password was reset successfully", e);
        }

        try {
            redisService.delete(buildRedisKey(account.getUsername()));
        } catch (Exception e) {
            log.warn("Failed to invalidate refresh tokens, but password was reset successfully", e);
        }

        log.info("Password reset completed successfully for user: {}", account.getUsername());
    }

    // ================= PRIVATE HELPERS ================= //
    private void validateField(RegisterRequest request) {
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }
    }

    private User buildProfileUser(RegisterRequest request, Account savedAccount) {
        return User.builder()
                .account(savedAccount)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    private AccountHasRole buildAccountRole(Account account, Role userRole) {
        return AccountHasRole.builder()
                .account(account)
                .role(userRole)
                .build();
    }

    private Account buildAccount(RegisterRequest request) {
        return Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
    }

    private Account authenticationAndValidateAccount(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomAccountDetails accountDetails = (CustomAccountDetails) authentication.getPrincipal();
        Account account = accountDetails.account();

        if (!account.isEmailVerified()) {
            throw new BadRequestException("Email is not verified. Please verify your email first.");
        }
        if (!UserStatus.ACTIVE.equals(account.getStatus())) {
            throw new BadRequestException("Account is not active");
        }

        return account;
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        String cleaned = token.trim();
        return cleaned.replaceAll("^\"|\"$", "");
    }

    private void verifyStoredRefreshToken(String username, String token) {
        String redisKey = buildRedisKey(username);
        Object storedTokenObj = redisService.get(redisKey);
        if (storedTokenObj == null) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String storedToken = storedTokenObj.toString().trim().replaceAll("^\"|\"$", "");
        if (!storedToken.equals(token)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
    }

    private String buildRedisKey(String username) {
        return "auth:refreshToken:" + username;
    }
}
