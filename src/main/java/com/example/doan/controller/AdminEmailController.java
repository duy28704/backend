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
        boolean isCustomMode = request.getCustomSubject() != null && !request.getCustomSubject().isBlank()
                && request.getCustomBody() != null && !request.getCustomBody().isBlank();

        EmailTemplateDTO template = null;
        if (!isCustomMode) {
            template = templateService.getTemplateByCode(request.getTemplateCode());
            if (template == null) {
                throw new RuntimeException("Mẫu email không tồn tại: " + request.getTemplateCode());
            }
        }

        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().contains("@"))
                .collect(Collectors.toList());

        EmailCampaign campaign = EmailCampaign.builder()
                .name(request.getName())
                .templateCode(isCustomMode ? "CUSTOM" : request.getTemplateCode())
                .targetGroup(request.getTargetGroup())
                .totalRecipients(recipients.size())
                .status("PROCESSING")
                .createdAt(Instant.now())
                .build();

        campaignRepository.save(campaign);

        // Queue emails for all target recipients
        for (User u : recipients) {
            String finalSubject;
            String finalBody;

            if (isCustomMode) {
                finalSubject = request.getCustomSubject();
                // Build professional marketing HTML from plain text + optional banner
                finalBody = buildMarketingHtml(
                        request.getCustomBody(),
                        request.getBannerImageUrl(),
                        u.getName() != null ? u.getName() : "Khách hàng"
                );
            } else {
                finalBody = templateService.renderContent(template.getBodyHtml(), Map.of(
                        "customerName", u.getName() != null ? u.getName() : "Khách hàng",
                        "promoUrl", "http://localhost:5173/shop"
                ));
                finalSubject = templateService.renderContent(template.getSubjectTemplate(), Map.of(
                        "customerName", u.getName() != null ? u.getName() : "Khách hàng"
                ));
            }

            EmailQueue queueItem = EmailQueue.builder()
                    .templateCode(isCustomMode ? "CUSTOM" : template.getCode())
                    .recipientEmail(u.getEmail())
                    .subject(finalSubject)
                    .bodyHtml(finalBody)
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

    /**
     * Converts admin plain text + optional banner image into a professional marketing HTML email.
     */
    private String buildMarketingHtml(String plainText, String bannerImageUrl, String customerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background-color:#f4f4f7;font-family:Arial,Helvetica,sans-serif;'>");
        sb.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f4f7;padding:30px 0;'><tr><td align='center'>");
        sb.append("<table role='presentation' width='600' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);'>");

        // Banner image
        if (bannerImageUrl != null && !bannerImageUrl.isBlank()) {
            sb.append("<tr><td style='padding:0;'>");
            sb.append("<img src='").append(bannerImageUrl.trim()).append("' alt='Banner' style='display:block;width:100%;max-width:600px;height:auto;border:0;' />");
            sb.append("</td></tr>");
        }

        // Header with logo
        sb.append("<tr><td style='padding:28px 32px 0 32px;text-align:center;'>");
        sb.append("<h1 style='font-size:22px;color:#0f62fe;margin:0 0 4px 0;letter-spacing:-0.5px;'>NEXUS Tech</h1>");
        sb.append("<p style='font-size:12px;color:#899bbd;margin:0;'>Premium Laptop & Technology Store</p>");
        sb.append("</td></tr>");

        // Greeting
        sb.append("<tr><td style='padding:24px 32px 0 32px;'>");
        sb.append("<p style='font-size:15px;color:#333;margin:0;'>Chào <strong>").append(customerName).append("</strong>,</p>");
        sb.append("</td></tr>");

        // Body content (convert line breaks to paragraphs)
        sb.append("<tr><td style='padding:16px 32px 24px 32px;'>");
        String[] paragraphs = plainText.split("\\n");
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                sb.append("<p style='font-size:15px;line-height:1.7;color:#444;margin:0 0 12px 0;'>").append(trimmed).append("</p>");
            }
        }
        sb.append("</td></tr>");

        // CTA Button
        sb.append("<tr><td style='padding:0 32px 28px 32px;text-align:center;'>");
        sb.append("<a href='http://localhost:5173/#shop' style='display:inline-block;padding:14px 36px;background-color:#0f62fe;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:bold;font-size:14px;letter-spacing:0.3px;'>Khám Phá Ngay Tại NEXUS</a>");
        sb.append("</td></tr>");

        // Footer
        sb.append("<tr><td style='padding:20px 32px;background-color:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;'>");
        sb.append("<p style='font-size:11px;color:#94a3b8;margin:0;'>Bạn nhận được email này vì đã đăng ký nhận thông tin từ NEXUS Tech.</p>");
        sb.append("<p style='font-size:11px;color:#94a3b8;margin:4px 0 0 0;'>© 2026 NEXUS Tech. All rights reserved.</p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
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
