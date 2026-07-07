package com.example.doan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogDTO {
    private Long id;
    private Long queueId;
    private String recipientEmail;
    private String subject;
    private String templateCode;
    private String status;
    private String errorDetails;
    private Instant sentAt;
}
