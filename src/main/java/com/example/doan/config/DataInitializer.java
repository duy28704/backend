package com.example.doan.config;

import com.example.doan.entity.User;
import com.example.doan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
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
            System.out.println("[DataInitializer] Successfully initialized Admin account: email=" + adminEmail + ", password=Password123");
        } else {
            System.out.println("[DataInitializer] Admin account already exists, skipping initialization.");
        }
    }
}
