package com.userservice.service;

import com.userservice.model.User;
import com.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;



    @Transactional
    public void generateAndSendOtpForEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email not found"));
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);
        sendOtpEmail(email, otp, OTP_EXPIRY_MINUTES);
        log.info("Generated and sent OTP for email: {}", email);
    }



    @Transactional
    public void generateAndSendOtpForUser(User user) {
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);
        sendOtpEmail(user.getEmail(), otp, OTP_EXPIRY_MINUTES);
        log.info("Generated and sent OTP for user: {}", user.getUsername());
    }

    private String generateOtp() {
        int number = random.nextInt(900000) + 100000; // ensures 6 digits
        return String.valueOf(number);
    }

    private void sendOtpEmail(String toEmail, String otp, int validityMinutes) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Your verification OTP");
        msg.setText("Your OTP is: " + otp + "\nThis OTP is valid for " + validityMinutes + " minutes.");
        mailSender.send(msg);
        log.debug("Sent OTP email to: {}", toEmail);
    }



    @Transactional
    public boolean verifyOtpForEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for email: {}", email);
            return false;
        }

        if (!user.getOtp().equals(otp)) {
            log.warn("Invalid OTP for email: {}", email);
            return false;
        }

        // success: mark verified and clear otp
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        log.info("User {} successfully verified via OTP", email);
        return true;
    }



    @Transactional
    public boolean verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null) {
            log.warn("No OTP found for password reset: {}", email);
            return false;
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for password reset: {}", email);
            return false;
        }

        if (!user.getOtp().equals(otp)) {
            log.warn("Invalid OTP for password reset: {}", email);
            return false;
        }

        // success -> encode new password, clear otp
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        log.info("Password successfully reset for user: {}", email);
        return true;
    }
}