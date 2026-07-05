package com.example.doan.config;

import com.example.doan.entity.User;
import com.example.doan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@nexus.com";
        String adminUsername = "admin";
        
        if (!userRepository.existsByEmail(adminEmail) && !userRepository.existsByUsername(adminUsername)) {
            String joinedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .name("System Administrator")
                    .password(passwordEncoder.encode("Password123"))
                    .role("ADMIN")
                    .enabled(true)
                    .accountNonLocked(true)
                    .deleted(false)
                    .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=150")
                    .joinedDate(joinedDate)
                    .createdAt(LocalDateTime.now())
                    .createdBy("system")
                    .build();
            
            userRepository.save(admin);
            log.info("Khởi tạo tài khoản Admin thành công: email={}, password=Password123", adminEmail);
        } else {
            log.info("Tài khoản Admin đã tồn tại trên hệ thống, bỏ qua bước khởi tạo.");
        }
    }
}
