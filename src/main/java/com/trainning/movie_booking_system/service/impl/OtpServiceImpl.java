package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.service.MailService;
import com.trainning.movie_booking_system.service.OtpService;
import com.trainning.movie_booking_system.service.RedisService;
import com.trainning.movie_booking_system.utils.enums.OtpType;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final RedisService redisService;
    private final MailService mailService;
    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${otp.ttl-minutes}")
    private long ttlMinutes;

    @Value("${otp.resend-limit-seconds}")
    private long resendLimitSeconds;

    @Value("${otp.count-ttl-hours}")
    private long countTtlHours;

    @Value("${otp.max-send-per-day}")
    private int maxSendPerDay;

    /**
     * Sinh ra mã OTP và lưu vào Redis với TTL (VD: 10 phút)
     *
     * @param email email account
     * @param type  loại OTP (REGISTER, FORGOT_PASSWORD...)
     * @return mã OTP
     */
    @Override
    public String generateOtpCode(String email, OtpType type) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redisService.set(buildOtpKey(email, type), otp, ttlMinutes, TimeUnit.MINUTES);
        return otp;
    }

    /**
     * Gửi mã OTP đến user
     *
     * @param email email
     * @param type  tpye
     */
    @Override
    public void sendOtp(String email, OtpType type) {
        log.info("Sending otp code for email: {}, type: {}", email, type.name().toLowerCase());

        if (type == null) {
            throw new BadRequestException("OTP type is required");
        }

        if (type == OtpType.REGISTER) {
            Account account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("Account not found"));
            if (account.isEmailVerified()) {
                throw new BadRequestException("Email already verified. Cannot resend REGISTER OTP.");
            }
        }

        String countKey = buildCountKey(email);
        String lastSendKey = buildLastSendKey(email, type);

        long count = Optional.ofNullable(redisService.get(countKey))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(0L);

        if (count >= maxSendPerDay) {
            throw new BadRequestException("You have exceeded the number of OTP sent per day");
        }

        if (redisService.exists(lastSendKey)) {
            throw new BadRequestException("Please wait before resending OTP");
        }

        String otp = generateOtpCode(email, type);
        mailService.sendSimpleEmailAsync(email, "Mã OTP của bạn", "Mã OTP: " + otp);

        redisService.set(countKey, count + 1, countTtlHours, TimeUnit.HOURS);
        redisService.set(lastSendKey, "sent", resendLimitSeconds, TimeUnit.SECONDS);
    }

    /**
     * Đếm số lần gửi OTP trong 24h
     *
     * @param email email account
     * @return tổng số lần gửi trong 24h
     */
    @Override
    public long countSendOtp(String email) {
        Object countVal = redisService.get(buildCountKey(email));
        return countVal == null ? 0 : Long.parseLong(countVal.toString());
    }

    /**
     * Xác thực OTP người dùng nhập
     *
     * @param email   email account
     * @param otpCode mã OTP người dùng nhập
     * @param type    loại OTP
     * @return true nếu đúng và chưa hết hạn
     */
    @Override
    public boolean verifyOtp(String email, String otpCode, OtpType type) {
        Object stored = redisService.get(buildOtpKey(email, type));
        return stored != null && stored.toString().equals(otpCode);
    }

    /**
     * Xóa OTP sau khi verify thành công
     *
     * @param email email
     * @param type type
     */
    @Override
    public void deleteOtp(String email, OtpType type) {
        redisService.delete(buildOtpKey(email, type));
    }

    //========== PRIVATE METHOD ==========/
    private String buildOtpKey(String email, OtpType type) {
        return "otp:%s:%s".formatted(type.name().toLowerCase(), email);
    }

    private String buildCountKey(String email) {
        return "otp_count:%s".formatted(email);
    }

    private String buildLastSendKey(String email, OtpType type) {
        return "otp_last_send:%s:%s".formatted(type.name().toLowerCase(), email);
    }
}
