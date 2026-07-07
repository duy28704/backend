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
public class EmailTemplateDTO {
    private Long id;
    private String code;
    private String name;
    private String subjectTemplate;
    private String bodyHtml;
    private String variablesJson;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
