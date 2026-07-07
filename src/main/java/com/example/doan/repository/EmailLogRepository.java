package com.example.doan.repository;

import com.example.doan.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findByRecipientEmailContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrderBySentAtDesc(String recipient, String subject, Pageable pageable);
    Page<EmailLog> findByStatusOrderBySentAtDesc(String status, Pageable pageable);
    
    long countByStatus(String status);

    @Query("SELECT DATE(l.sentAt) as sentDate, COUNT(l) as total, SUM(CASE WHEN l.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount, SUM(CASE WHEN l.status = 'FAILED' THEN 1 ELSE 0 END) as failedCount FROM EmailLog l WHERE l.sentAt >= :since GROUP BY DATE(l.sentAt) ORDER BY DATE(l.sentAt) ASC")
    List<Object[]> getDailyStatsSince(Instant since);
}
