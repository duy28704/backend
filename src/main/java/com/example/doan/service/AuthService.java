package com.example.doan.service;

import com.example.doan.dto.LoginRequest;
import com.example.doan.dto.RefreshTokenRequest;
import com.example.doan.dto.RegisterRequest;
import com.example.doan.dto.UpdateProfileRequest;
import com.example.doan.entity.User;
import com.example.doan.repository.UserRepository;
import com.example.doan.response.ApiResponse;
import com.example.doan.response.AuthResponse;
import com.example.doan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.example.doan.entity.Captcha;
import com.example.doan.entity.Otp;
import com.example.doan.repository.CaptchaRepository;
import com.example.doan.repository.OtpRepository;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final CaptchaRepository captchaRepository;
    private final OtpRepository otpRepository;
    private final TurnstileService turnstileService;
    private final com.example.doan.repository.EmailQueueRepository emailQueueRepository;

    public Map<String, Object> generateCaptcha() {
        java.time.Instant now = java.time.Instant.now();
        // Evict expired captchas in DB
        try {
            captchaRepository.deleteByExpiryBefore(now);
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp captcha hết hạn: {}", e.getMessage());
        }

        int a = (int) (Math.random() * 9) + 1; // 1 to 9
        int b = (int) (Math.random() * 9) + 1; // 1 to 9
        String captchaId = UUID.randomUUID().toString();
        String question = a + " + " + b + " = ?";
        String answer = String.valueOf(a + b);

        Captcha captcha = Captcha.builder()
                .id(captchaId)
                .answer(answer)
                .expiry(now.plusSeconds(120)) // 2 minutes
                .build();
        captchaRepository.save(captcha);

        Map<String, Object> response = new HashMap<>();
        response.put("captchaId", captchaId);
        response.put("question", question);
        return response;
    }

    public boolean validateCaptcha(String captchaId, String answer) {
        if (captchaId == null || answer == null) return false;

        java.time.Instant now = java.time.Instant.now();
        Captcha captcha = captchaRepository.findById(captchaId).orElse(null);
        if (captcha == null || captcha.getExpiry().isBefore(now)) {
            if (captcha != null) {
                captchaRepository.delete(captcha);
            }
            return false;
        }

        boolean isValid = captcha.getAnswer().trim().equals(answer.trim());

        // Single use captcha - delete immediately
        captchaRepository.delete(captcha);
        return isValid;
    }

    // =========================
    // REGISTER
    // =========================
    public ApiResponse<AuthResponse> register(
            RegisterRequest request
    ) {
        String email = request.getEmail();
        if (email == null) {
            log.warn("Đăng ký thất bại: email bị trống");
            throw new RuntimeException("Email is required");
        }
        
        log.info("Bắt đầu xử lý đăng ký tài khoản mới với email: {}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("Đăng ký thất bại: email này đã tồn tại trên hệ thống: {}", email);
            throw new RuntimeException("Địa chỉ email này đã tồn tại trên hệ thống.");
        }
        
        String username = request.getUsername() != null ? request.getUsername() : email;
        if (userRepository.existsByUsername(username)) {
            log.warn("Đăng ký thất bại: tên đăng nhập này đã tồn tại: {}", username);
            throw new RuntimeException("Username already exists");
        }
        
        String name = request.getName() != null ? request.getName() : request.getFullName();
        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }
        
        String joinedDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        User user = User.builder()
                .username(username)
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("CUSTOMER")
                .enabled(true)
                .accountNonLocked(true)
                .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=150")
                .joinedDate(joinedDate)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("Khởi tạo đăng ký người dùng mới thành công: tên đăng nhập={}, email={}", username, email);

        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .permissions(user.getActivePermissions())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .dob(user.getDob())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .joinedDate(user.getJoinedDate())
                .message("Register Success")
                .build();

        return ApiResponse.<AuthResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(201)
                .message("Register Success")
                .data(authResponse)
                .build();
    }

    // =========================
    // LOGIN
    // =========================
    public ApiResponse<AuthResponse> login(
            LoginRequest request
    ) {
        // 1. Validate CAPTCHA (Cloudflare Turnstile token or fallback text CAPTCHA)
        boolean isCaptchaValid = false;
        if (request.getTurnstileToken() != null && !request.getTurnstileToken().trim().isEmpty()) {
            isCaptchaValid = turnstileService.verifyToken(request.getTurnstileToken());
        } else {
            isCaptchaValid = validateCaptcha(request.getCaptchaId(), request.getCaptchaAnswer());
        }

        if (!isCaptchaValid) {
            log.warn("Đăng nhập thất bại: Xác minh bảo mật (CAPTCHA) không chính xác hoặc đã hết hạn.");
            throw new RuntimeException("Xác minh bảo mật (CAPTCHA) không thành công. Vui lòng thử lại.");
        }

        String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
        if (identifier == null) {
            log.warn("Đăng nhập thất bại: thiếu thông tin tài khoản (email hoặc tên đăng nhập)");
            throw new RuntimeException("Email hoặc Username là bắt buộc.");
        }

        log.info("Bắt đầu xử lý đăng nhập cho tài khoản: {}", identifier);
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Không tìm thấy tài khoản với thông tin đăng nhập: {}", identifier);
                    return new RuntimeException("Tài khoản không tồn tại trên hệ thống. Vui lòng đăng ký tài khoản mới.");
                });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.warn("Đăng nhập thất bại: Mật khẩu không chính xác cho người dùng={}", user.getUsername());
            throw new RuntimeException("Mật khẩu đăng nhập không chính xác. Vui lòng kiểm tra lại.");
        }

        // 2. Generate and send OTP
        String otpCode = String.format("%06d", (int) (Math.random() * 1000000));
        java.time.Instant now = java.time.Instant.now();
        // Evict expired OTPs in DB
        try {
            otpRepository.deleteByExpiryBefore(now);
        } catch (Exception e) {}
        
        // Remove previous OTPs for this user
        otpRepository.deleteByUsername(user.getUsername());

        Otp otp = Otp.builder()
                .username(user.getUsername())
                .code(otpCode)
                .expiry(now.plusSeconds(300)) // valid for 5 minutes
                .build();
        otpRepository.save(otp);

        emailService.sendOtp(user.getEmail(), otpCode);

        try {
            emailQueueRepository.save(com.example.doan.entity.EmailQueue.builder()
                    .templateCode("OTP_LOGIN")
                    .recipientEmail(user.getEmail())
                    .subject("Mã xác thực OTP đăng nhập NEXUS Tech: " + otpCode)
                    .bodyHtml("<p>Mã xác thực OTP đăng nhập vào hệ thống NEXUS Tech của bạn là: <strong>" + otpCode + "</strong></p>")
                    .status("SENT")
                    .sentAt(java.time.Instant.now())
                    .createdAt(java.time.Instant.now())
                    .build());
        } catch (Exception e) {}

        AuthResponse authResponse = AuthResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .message("OTP_REQUIRED")
                .build();

        return ApiResponse.<AuthResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("OTP_REQUIRED")
                .data(authResponse)
                .build();
    }

    public ApiResponse<AuthResponse> verifyOtp(String username, String otp) {
        if (username == null || otp == null) {
            throw new RuntimeException("Tên đăng nhập và mã OTP là bắt buộc.");
        }

        java.time.Instant now = java.time.Instant.now();
        Otp otpRecord = otpRepository.findTopByUsernameOrderByExpiryDesc(username).orElse(null);
        if (otpRecord == null || otpRecord.getExpiry().isBefore(now)) {
            if (otpRecord != null) {
                otpRepository.delete(otpRecord);
            }
            throw new RuntimeException("Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng gửi lại.");
        }

        if (!otpRecord.getCode().equals(otp.trim())) {
            throw new RuntimeException("Mã OTP nhập vào không chính xác.");
        }

        // Consume OTP
        otpRepository.delete(otpRecord);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản người dùng không tồn tại."));

        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .permissions(user.getActivePermissions())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .dob(user.getDob())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .joinedDate(user.getJoinedDate())
                .message("Login Success")
                .build();

        return ApiResponse.<AuthResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Login Success")
                .data(authResponse)
                .build();
    }

    // =========================
    // LOGOUT
    // =========================
    public ApiResponse<?> logout(
            String token
    ) {
        String username = null;
        try {
            if (token != null && token.startsWith("Bearer ")) {
                username = jwtUtil.extractUsername(token.substring(7));
            }
        } catch (Exception e) {
            // Bỏ qua lỗi phân tích mã token khi đăng xuất
        }
        log.info("Yêu cầu đăng xuất tài khoản: người dùng={}", (username != null ? username : "không xác định"));
        return ApiResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Logout Success")
                .data(null)
                .build();
    }

    // =========================
    // UPDATE PROFILE
    // =========================
    public ApiResponse<AuthResponse> updateProfile(
            String token,
            UpdateProfileRequest request
    ) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String username = jwtUtil.extractUsername(token);
        log.info("Bắt đầu xử lý cập nhật thông tin hồ sơ cho người dùng={}", username);
        
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> {
                    log.warn("Cập nhật hồ sơ thất bại: không tìm thấy tài khoản cho tên đăng nhập={}", username);
                    return new RuntimeException("Không tìm thấy tài khoản người dùng.");
                });

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Cập nhật thông tin hồ sơ người dùng thành công: {}", user.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .role(user.getRole())
                .permissions(user.getActivePermissions())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .dob(user.getDob())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .joinedDate(user.getJoinedDate())
                .message("Update Profile Success")
                .build();

        return ApiResponse.<AuthResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Update Profile Success")
                .data(authResponse)
                .build();
    }

    // =========================
    // REFRESH TOKEN
    // =========================
    public ApiResponse<AuthResponse> refreshToken(
            RefreshTokenRequest request
    ) {
        String username = jwtUtil.extractUsername(request.getRefreshToken());
        log.info("Bắt đầu xử lý làm mới mã token cho người dùng={}", username);

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng."));

        String newAccessToken = jwtUtil.generateToken(username);
        log.info("Làm mới mã token thành công cho người dùng={}", username);

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .role(user.getRole())
                .permissions(user.getActivePermissions())
                .username(username)
                .message("Refresh Token Success")
                .build();

        return ApiResponse.<AuthResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Refresh Token Success")
                .data(response)
                .build();
    }

    public ApiResponse<Void> resendOtp(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản người dùng không tồn tại."));

        // Generate new OTP
        String otpCode = String.format("%06d", (int) (Math.random() * 1000000));
        java.time.Instant now = java.time.Instant.now();
        // Remove previous OTPs for this user
        otpRepository.deleteByUsername(user.getUsername());

        Otp otp = Otp.builder()
                .username(user.getUsername())
                .code(otpCode)
                .expiry(now.plusSeconds(300)) // 5 mins
                .build();
        otpRepository.save(otp);

        emailService.sendOtp(user.getEmail(), otpCode);

        return ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("OTP_RESENT")
                .build();
    }
}


