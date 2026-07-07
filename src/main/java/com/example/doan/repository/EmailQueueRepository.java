package com.example.doan.repository;

import com.example.doan.entity.EmailQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {
    
    @Query("SELECT q FROM EmailQueue q WHERE (q.status = 'PENDING' OR (q.status = 'FAILED' AND q.retryCount < q.maxRetries)) AND q.scheduledAt <= :now ORDER BY q.createdAt ASC")
    List<EmailQueue> findNextBatchToProcess(Instant now, Pageable pageable);

    long countByStatus(String status);
}
