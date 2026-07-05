package com.example.doan.controller;

import com.example.doan.dto.LoginRequest;
import com.example.doan.dto.RefreshTokenRequest;
import com.example.doan.dto.RegisterRequest;
import com.example.doan.dto.UpdateProfileRequest;
import com.example.doan.response.ApiResponse;
import com.example.doan.response.AuthResponse;
import com.example.doan.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody RegisterRequest request
    ) {
        ApiResponse<AuthResponse> response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // UPDATE PROFILE
    // =========================
    @PutMapping("/profile/update")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateProfileRequest request
    ) {
        ApiResponse<AuthResponse> response =
                authService.updateProfile(token, request);

        return ResponseEntity.ok(response);
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>>
    login(

            @RequestBody LoginRequest request

    ) {

        ApiResponse<AuthResponse> response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    // =========================
    // CAPTCHA
    // =========================
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCaptcha() {
        java.util.Map<String, Object> captchaData = authService.generateCaptcha();
        ApiResponse<java.util.Map<String, Object>> response = ApiResponse.<java.util.Map<String, Object>>builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Generate captcha success")
                .data(captchaData)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // VERIFY OTP
    // =========================
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @RequestParam String username,
            @RequestParam String otp
    ) {
        ApiResponse<AuthResponse> response = authService.verifyOtp(username, otp);
        return ResponseEntity.ok(response);
    }

    // =========================
    // LOGOUT
    // =========================
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>>
    logout(

            @RequestHeader("Authorization")
            String token

    ) {

        ApiResponse<?> response =
                authService.logout(token);

        return ResponseEntity.ok(response);
    }

    // =========================
    // REFRESH TOKEN
    // =========================
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {

        ApiResponse<AuthResponse> response =
                authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    // =========================
    // RESEND OTP
    // =========================
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String username) {
        ApiResponse<Void> response = authService.resendOtp(username);
        return ResponseEntity.ok(response);
    }
}
