package com.example.doan.controller;

import com.example.doan.entity.User;
import com.example.doan.repository.UserRepository;
import com.example.doan.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================
    // GET ALL USERS
    // =========================
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        ApiResponse<List<User>> response = ApiResponse.<List<User>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get all users success")
                .data(users)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // CREATE NEW USER
    // =========================
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            request.setUsername(request.getEmail());
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Địa chỉ email này đã tồn tại trên hệ thống.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập này đã tồn tại.");
        }
        
        String joinedDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .name(request.getName() != null ? request.getName().trim() : "User")
                .password(passwordEncoder.encode(request.getPassword() != null && !request.getPassword().trim().isEmpty() ? request.getPassword() : "Password123"))
                .role(request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER")
                .phone(request.getPhone())
                .address(request.getAddress())
                .dob(request.getDob())
                .gender(request.getGender())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=150")
                .joinedDate(joinedDate)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .accountNonLocked(true)
                .createdAt(LocalDateTime.now())
                .build();
                
        userRepository.save(user);
        
        ApiResponse<User> response = ApiResponse.<User>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Create user success")
                .data(user)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================
    // UPDATE USER
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @RequestBody User request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này."));
                
        if (request.getName() != null) user.setName(request.getName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) user.setAddress(request.getAddress().trim());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        ApiResponse<User> response = ApiResponse.<User>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Update user success")
                .data(user)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // DELETE USER
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này."));
        userRepository.delete(user);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Delete user success")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
