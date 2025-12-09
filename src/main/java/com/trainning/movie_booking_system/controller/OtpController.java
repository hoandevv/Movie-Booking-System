package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Otp.SendOtpRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OtpController {

    private final OtpService otpService;

    /**
     * Send/Resend OTP to email
     * PUBLIC - No authentication required
     * POST /api/v1/otp/send
     * 
     * @param request email and OTP type
     * @return success message
     */
    @PostMapping("/send")
    public ResponseEntity<?> resend(@RequestBody @Valid SendOtpRequest request) {
        log.info("[OTP] Send OTP to email: {}", request.getEmail());
        otpService.sendOtp(request.getEmail(), request.getType());
        return ResponseEntity.ok(BaseResponse.success());
    }
}
