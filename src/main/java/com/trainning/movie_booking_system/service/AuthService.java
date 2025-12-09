package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ForgotPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ResetPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;

public interface AuthService {
    /**
     * User tạo tài khoản mới
     * @param request thông tin cá nhân
     */
    void register(RegisterRequest request);

    /**
     * Active email
     * @param request thông tin request
     */
    void activateAccount(VerifyOtpRequest request);

    /**
     * User đăng nhập
     * @param request thông tin đăng nhập
     * @return AuthResponse chứa access token và refresh token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refresh token lấy access token mới
     * @param refreshToken refresh token
     * @return Access and refresh token
     */
    AuthResponse refreshToken(String refreshToken);

    /**
<<<<<<< HEAD
     * Logout: invalidate refresh token
     * @param refreshToken refresh token
=======
     * Logout: invalidate the refresh token
     * @param refreshToken refresh token to be invalidated
>>>>>>> 27efa744756701f56c10ae702c2fe328749d193d
     */
    void logout(String refreshToken);

    /**
     * Forgot password: send OTP to email using Redis-backed OTP storage
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password: verify OTP then update password and invalidate refresh token
     */
    void resetPassword(ResetPasswordRequest request);
}
