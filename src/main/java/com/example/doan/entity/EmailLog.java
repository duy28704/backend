package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue_id")
    private Long queueId;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Builder.Default
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt = Instant.now();
}
