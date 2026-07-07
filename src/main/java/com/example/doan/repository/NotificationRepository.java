package com.example.doan.repository;

import com.example.doan.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername, Pageable pageable);
    Page<Notification> findByRecipientUsernameAndIsReadOrderByCreatedAtDesc(String recipientUsername, boolean isRead, Pageable pageable);
    long countByRecipientUsernameAndIsReadFalse(String recipientUsername);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipientUsername = :username AND n.isRead = false")
    int markAllAsReadForUser(String username, Instant now);
}
