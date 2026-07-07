package com.example.doan.controller;

import com.example.doan.dto.*;
import com.example.doan.entity.EmailCampaign;
import com.example.doan.entity.EmailLog;
import com.example.doan.entity.EmailQueue;
import com.example.doan.entity.User;
import com.example.doan.repository.EmailCampaignRepository;
import com.example.doan.repository.EmailLogRepository;
import com.example.doan.repository.EmailQueueRepository;
import com.example.doan.repository.UserRepository;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.EmailAnalyticsService;
import com.example.doan.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/emails")
@RequiredArgsConstructor
@Slf4j
public class AdminEmailController {

    private final EmailTemplateService templateService;
    private final EmailLogRepository logRepository;
    private final EmailQueueRepository queueRepository;
    private final EmailCampaignRepository campaignRepository;
    private final EmailAnalyticsService analyticsService;
    private final UserRepository userRepository;

    // --- TEMPLATES ---
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<EmailTemplateDTO>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.<List<EmailTemplateDTO>>builder()
                .status(200)
                .message("Success")
                .data(templateService.getAllTemplates())
                .build());
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<EmailTemplateDTO>> saveTemplate(@RequestBody EmailTemplateDTO dto) {
        EmailTemplateDTO saved = templateService.saveTemplate(dto);
        return ResponseEntity.ok(ApiResponse.<EmailTemplateDTO>builder()
                .status(200)
                .message("Save template success")
                .data(saved)
                .build());
    }

    // --- LOGS ---
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<EmailLogDTO>>> getLogs(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<EmailLog> logsPage;
        if (query != null && !query.trim().isEmpty()) {
            logsPage = logRepository.findByRecipientEmailContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrderBySentAtDesc(query.trim(), query.trim(), pageable);
        } else {
            logsPage = logRepository.findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by("sentAt").descending()));
        }

        Page<EmailLogDTO> dtoPage = logsPage.map(entity -> EmailLogDTO.builder()
                .id(entity.getId())
                .queueId(entity.getQueueId())
                .recipientEmail(entity.getRecipientEmail())
                .subject(entity.getSubject())
                .templateCode(entity.getTemplateCode())
                .status(entity.getStatus())
                .errorDetails(entity.getErrorDetails())
                .sentAt(entity.getSentAt())
                .build());

        return ResponseEntity.ok(ApiResponse.<Page<EmailLogDTO>>builder()
                .status(200)
                .message("Success")
                .data(dtoPage)
                .build());
    }

    @PostMapping("/logs/{id}/resend")
    public ResponseEntity<ApiResponse<Void>> resendEmail(@PathVariable Long id) {
        EmailLog logEntry = logRepository.findById(id).orElseThrow(() -> new RuntimeException("Log không tồn tại"));

        // Push new item into queue
        EmailQueue queueItem = EmailQueue.builder()
                .templateCode(logEntry.getTemplateCode())
                .recipientEmail(logEntry.getRecipientEmail())
                .subject(logEntry.getSubject())
                .bodyHtml("<p>Re-sent email for: " + logEntry.getSubject() + "</p>")
                .status("PENDING")
                .scheduledAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        queueRepository.save(queueItem);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(200)
                .message("Re-queued email for resending successfully")
                .build());
    }

    // --- CAMPAIGNS ---
    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<Page<EmailCampaign>>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<EmailCampaign> data = campaignRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.<Page<EmailCampaign>>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build());
    }

    @PostMapping("/campaigns")
    public ResponseEntity<ApiResponse<EmailCampaign>> createCampaign(@RequestBody CreateCampaignRequest request) {
        EmailTemplateDTO template = templateService.getTemplateByCode(request.getTemplateCode());
        if (template == null) {
            throw new RuntimeException("Mẫu email không tồn tại: " + request.getTemplateCode());
        }

        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().contains("@"))
                .collect(Collectors.toList());

        EmailCampaign campaign = EmailCampaign.builder()
                .name(request.getName())
                .templateCode(request.getTemplateCode())
                .targetGroup(request.getTargetGroup())
                .totalRecipients(recipients.size())
                .status("PROCESSING")
                .createdAt(Instant.now())
                .build();

        campaignRepository.save(campaign);

        // Queue emails for all target recipients
        for (User u : recipients) {
            String renderedBody = templateService.renderContent(template.getBodyHtml(), Map.of(
                    "customerName", u.getName() != null ? u.getName() : "Khách hàng",
                    "promoUrl", "http://localhost:5173/shop"
            ));

            String renderedSubject = templateService.renderContent(template.getSubjectTemplate(), Map.of(
                    "customerName", u.getName() != null ? u.getName() : "Khách hàng"
            ));

            EmailQueue queueItem = EmailQueue.builder()
                    .templateCode(template.getCode())
                    .recipientEmail(u.getEmail())
                    .subject(renderedSubject)
                    .bodyHtml(renderedBody)
                    .status("PENDING")
                    .scheduledAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            queueRepository.save(queueItem);
        }

        campaign.setStatus("COMPLETED");
        campaign.setSentCount(recipients.size());
        campaign.setSuccessCount(recipients.size());
        campaignRepository.save(campaign);

        return ResponseEntity.ok(ApiResponse.<EmailCampaign>builder()
                .status(200)
                .message("Create mass campaign success")
                .data(campaign)
                .build());
    }

    // --- ANALYTICS ---
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<EmailAnalyticsDTO>> getAnalytics() {
        EmailAnalyticsDTO analytics = analyticsService.getAnalyticsSummary();
        return ResponseEntity.ok(ApiResponse.<EmailAnalyticsDTO>builder()
                .status(200)
                .message("Success")
                .data(analytics)
                .build());
    }
}
