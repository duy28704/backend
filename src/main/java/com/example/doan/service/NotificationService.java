package com.example.doan.service;

import com.example.doan.dto.NotificationDTO;
import com.example.doan.entity.Notification;
import com.example.doan.entity.NotificationType;
import com.example.doan.repository.NotificationRepository;
import com.example.doan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationDTO createNotification(
            String recipientUsername,
            String title,
            String content,
            NotificationType type,
            String referenceId,
            String referenceUrl
    ) {
        if (recipientUsername == null || title == null) return null;

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .recipientUsername(recipientUsername)
                .title(title)
                .content(content)
                .type(type)
                .referenceId(referenceId)
                .referenceUrl(referenceUrl)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Khởi tạo thông báo mới cho {}: {}", recipientUsername, title);
        return mapToDTO(notification);
    }

    public void notifyAllAdmins(String title, String content, NotificationType type, String referenceId, String referenceUrl) {
        try {
            userRepository.findAll().stream()
                    .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()) || "STAFF".equalsIgnoreCase(u.getRole()))
                    .forEach(u -> createNotification(u.getUsername(), title, content, type, referenceId, referenceUrl));
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo tới Admins: {}", e.getMessage());
        }
    }

    public Page<NotificationDTO> getUserNotifications(String username, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> pageResult;
        if (unreadOnly) {
            pageResult = notificationRepository.findByRecipientUsernameAndIsReadOrderByCreatedAtDesc(username, false, pageable);
        } else {
            pageResult = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username, pageable);
        }
        return pageResult.map(this::mapToDTO);
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndIsReadFalse(username);
    }

    @Transactional
    public NotificationDTO markAsRead(String notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getRecipientUsername().equals(username)) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
                notificationRepository.save(notification);
            }
            return mapToDTO(notification);
        }
        return null;
    }

    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsReadForUser(username, Instant.now());
    }

    @Transactional
    public boolean deleteNotification(String notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getRecipientUsername().equals(username)) {
            notificationRepository.delete(notification);
            return true;
        }
        return false;
    }

    private NotificationDTO mapToDTO(Notification entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .recipientUsername(entity.getRecipientUsername())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .referenceUrl(entity.getReferenceUrl())
                .isRead(entity.isRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
