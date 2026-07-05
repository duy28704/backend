package com.example.doan.repository;

import com.example.doan.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    
    Optional<Otp> findTopByUsernameOrderByExpiryDesc(String username);
    
    @Transactional
    @Modifying
    void deleteByUsername(String username);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiry < :now")
    void deleteByExpiryBefore(Instant now);
}
