package com.userservice.controller;


import com.userservice.dto.*;
import com.userservice.security.JwtUtil;
import com.userservice.service.CustomUserDetailsService;
import com.userservice.service.OtpService;
import com.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final OtpService otpService;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil,
                          CustomUserDetailsService userDetailsService, UserService userService,
                          OtpService otpService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.otpService = otpService;
    }

    // Register - now will send OTP to verify email
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Validated @RequestBody UserCreateDto dto) {
        UserDto created = userService.registerUser(dto);
        // OTP send done inside UserService -> OtpService
        return ResponseEntity.ok(created);
    }

    @PostMapping("/bulk-register")
    public ResponseEntity<?> bulkRegister(@Validated @RequestBody BulkRegisterRequestDto request) {

        int successCount = 0;
        int failureCount = 0;

        List<Map<String, Object>> results = new ArrayList<>();

        for (UserCreateDto dto : request.getUsers()) {
            try {
                UserDto user = userService.registerUser(dto);
                successCount++;

                results.add(Map.of(
                        "username", dto.getUsername(),
                        "status", "SUCCESS",
                        "message", "User created. OTP sent to email."
                ));

            } catch (Exception e) {
                failureCount++;

                results.add(Map.of(
                        "username", dto.getUsername(),
                        "status", "FAILED",
                        "message", e.getMessage()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "summary", Map.of(
                        "total", request.getUsers().size(),
                        "success", successCount,
                        "failed", failureCount
                ),
                "results", results
        ));
    }


//    @PostMapping("/adminRegister")
//    public ResponseEntity<UserDto> adminRegister(@Validated @RequestBody UserCreateDto dto) {
//        UserDto created = userService.registerUser(dto);
//        // OTP send done inside UserService -> OtpService
//        return ResponseEntity.ok(created);
//    }

    // ✅ FIXED: Login with PRN in JWT token
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Validated @RequestBody AuthRequestDto request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // load user and ensure verified
            var user = userDetailsService.loadUserByUsername(request.getUsername());
            // NOTE: CustomUserDetailsService returns UserDetails even if not verified;
            // validateCredentials prevents login in service layer. Alternatively, check here:
            // find user DTO and ensure verified
            var userDto = userService.findByUsername(user.getUsername());
            if (userDto == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (!userDto.isVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(AuthResponseDto.builder().token(null).user(userDto).build());
            }

            String role = user.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

            // ✅ CRITICAL FIX: Pass PRN as third parameter
            String token = jwtUtil.generateToken(
                    user.getUsername(),
                    role,
                    userDto.getUsn()
            );

            AuthResponseDto resp = AuthResponseDto.builder().token(token).user(userDto).build();
            return ResponseEntity.ok(resp);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    // Validate credentials (no token) - useful if verifying before login
    @PostMapping("/validate-credentials")
    public ResponseEntity<?> validateCredentials(@RequestBody AuthRequestDto dto) {
        boolean ok = userService.validateCredentials(dto.getUsername(), dto.getPassword());
        return ResponseEntity.ok().body(Map.of("valid", ok));
    }

    // Validate token (unchanged)
    @GetMapping("/validate-token")
    public ResponseEntity<UserDto> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(400).build();
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return ResponseEntity.status(401).build();

        String username = jwtUtil.extractUsername(token);
        UserDto dto = userService.findByUsername(username);
        if (dto == null) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(dto);
    }

    // ==== OTP endpoints ====

    // Verify OTP after registration
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "email and otp required"));
        }
        boolean ok = otpService.verifyOtpForEmail(email, otp);
        if (ok) return ResponseEntity.ok(Map.of("message", "Email verified"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired OTP"));
    }

    @PostMapping("/bulk-verify-otp")
    public ResponseEntity<?> bulkVerifyOtp(@Validated @RequestBody BulkOtpVerifyRequestDto request) {

        int successCount = 0;
        int failureCount = 0;

        List<Map<String, Object>> results = new ArrayList<>();

        for (BulkOtpVerifyRequestDto.OtpItem item : request.getRequests()) {

            boolean ok = otpService.verifyOtpForEmail(item.getEmail(), item.getOtp());

            if (ok) {
                successCount++;
                results.add(Map.of(
                        "email", item.getEmail(),
                        "status", "VERIFIED"
                ));
            } else {
                failureCount++;
                results.add(Map.of(
                        "email", item.getEmail(),
                        "status", "FAILED",
                        "message", "Invalid or expired OTP"
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "summary", Map.of(
                        "total", request.getRequests().size(),
                        "verified", successCount,
                        "failed", failureCount
                ),
                "results", results
        ));
    }


    // Resend verification OTP (if user didn't get it)
    @PostMapping("/resend-verify-otp")
    public ResponseEntity<?> resendVerificationOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("message", "email required"));
        otpService.generateAndSendOtpForEmail(email);
        return ResponseEntity.ok(Map.of("message", "OTP resent"));
    }

    // Forgot password -> send OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("message", "email required"));
        otpService.generateAndSendOtpForEmail(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent for password reset"));
    }

    // Reset password using OTP (email + otp + newPassword)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");
        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "email, otp and newPassword required"));
        }
        boolean ok = otpService.verifyOtpAndResetPassword(email, otp, newPassword);
        if (ok) return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired OTP"));
    }
}