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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    // =========================
    // REGISTER
    // =========================
    public ApiResponse<AuthResponse> register(
            RegisterRequest request
    ) {
        String email = request.getEmail();
        if (email == null) {
            throw new RuntimeException("Email is required");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Địa chỉ email này đã tồn tại trên hệ thống.");
        }
        
        String username = request.getUsername() != null ? request.getUsername() : email;
        if (userRepository.existsByUsername(username)) {
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

        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
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
        String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
        if (identifier == null) {
            throw new RuntimeException("Email hoặc Username là bắt buộc.");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trên hệ thống. Vui lòng đăng ký tài khoản mới."));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Mật khẩu đăng nhập không chính xác. Vui lòng kiểm tra lại.");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
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
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng."));

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

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .role(user.getRole())
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

        String username =
                jwtUtil.extractUsername(
                        request.getRefreshToken()
                );

        String newAccessToken =
                jwtUtil.generateToken(username);

        AuthResponse response =
                AuthResponse.builder()

                        .accessToken(newAccessToken)

                        .refreshToken(
                                request.getRefreshToken()
                        )

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
}
