package com.example.doan.service;

import com.example.doan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(
            String usernameOrEmail
    ) throws UsernameNotFoundException {

        log.info("Tải thông tin người dùng từ hệ thống: '{}'", usernameOrEmail);
        return userRepository
                .findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(
                        () -> {
                            log.warn("Không tìm thấy người dùng trong hệ thống với tên đăng nhập hoặc email: '{}'", usernameOrEmail);
                            return new UsernameNotFoundException(
                                    "User Not Found with username or email: " + usernameOrEmail
                            );
                        }
                );
    }
}