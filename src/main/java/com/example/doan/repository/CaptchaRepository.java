package com.example.doan.repository;

import com.example.doan.entity.Captcha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Repository
public interface CaptchaRepository extends JpaRepository<Captcha, String> {
    
    @Transactional
    @Modifying
    @Query("DELETE FROM Captcha c WHERE c.expiry < :now")
    void deleteByExpiryBefore(Instant now);
}
