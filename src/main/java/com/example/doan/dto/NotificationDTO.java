package com.example.doan.dto;

import com.example.doan.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String recipientUsername;
    private String title;
    private String content;
    private NotificationType type;
    private String referenceId;
    private String referenceUrl;
    private boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}
