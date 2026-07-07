package com.example.doan.service;

import com.example.doan.dto.EmailTemplateDTO;
import com.example.doan.entity.EmailTemplate;
import com.example.doan.repository.EmailTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    @PostConstruct
    public void initDefaultTemplates() {
        try {
            if (!templateRepository.existsByCode("OTP_LOGIN")) {
                templateRepository.save(EmailTemplate.builder()
                        .code("OTP_LOGIN")
                        .name("Mẫu OTP Đăng Nhập")
                        .subjectTemplate("Mã xác thực OTP đăng nhập NEXUS Tech: {{otpCode}}")
                        .bodyHtml("<div style='font-family: Arial, sans-serif; max-width: 600px; padding: 20px; border: 1px solid #eee; border-radius: 8px;'>"
                                + "<h2 style='color: #ff003c;'>NEXUS Tech - Xác Thực Đăng Nhập</h2>"
                                + "<p>Chào <strong>{{customerName}}</strong>,</p>"
                                + "<p>Mã OTP xác thực đăng nhập vào hệ thống NEXUS Tech của bạn là:</p>"
                                + "<div style='font-size: 28px; font-weight: bold; color: #ff003c; letter-spacing: 4px; padding: 15px; background: #f8f9fa; text-align: center; border-radius: 6px;'>{{otpCode}}</div>"
                                + "<p style='color: #777; font-size: 13px; margin-top: 15px;'>Mã OTP có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>"
                                + "</div>")
                        .variablesJson("[\"customerName\", \"otpCode\"]")
                        .isActive(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
            }

            if (!templateRepository.existsByCode("ORDER_CONFIRMED")) {
                templateRepository.save(EmailTemplate.builder()
                        .code("ORDER_CONFIRMED")
                        .name("Mẫu Xác Nhận Đơn Hàng")
                        .subjectTemplate("NEXUS Tech - Xác nhận đơn hàng thành công #{{orderId}}")
                        .bodyHtml("<div style='font-family: Arial, sans-serif; max-width: 600px; padding: 20px; border: 1px solid #eee; border-radius: 8px;'>"
                                + "<h2 style='color: #28a745;'>Xác Nhận Đơn Hàng Thành Công!</h2>"
                                + "<p>Chào <strong>{{customerName}}</strong>,</p>"
                                + "<p>Cảm ơn bạn đã đặt hàng tại NEXUS Tech. Mã đơn hàng của bạn là: <strong>#{{orderId}}</strong></p>"
                                + "<p>Tổng giá trị thanh toán: <strong style='color: #ff003c;'>{{totalAmount}} ₫</strong></p>"
                                + "<p>Địa chỉ nhận hàng: {{address}}</p>"
                                + "<p style='color: #777; font-size: 13px;'>Chúng tôi đang chuẩn bị hàng và sẽ giao tới bạn trong thời gian sớm nhất.</p>"
                                + "</div>")
                        .variablesJson("[\"customerName\", \"orderId\", \"totalAmount\", \"address\"]")
                        .isActive(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
            }

            if (!templateRepository.existsByCode("PROMO_FLASH_SALE")) {
                templateRepository.save(EmailTemplate.builder()
                        .code("PROMO_FLASH_SALE")
                        .name("Mẫu Ưu Đãi Flash Sale")
                        .subjectTemplate("🔥 [NEXUS Tech] Siêu Khuyến Mãi Flash Sale Dành Cho Bạn!")
                        .bodyHtml("<div style='font-family: Arial, sans-serif; max-width: 600px; padding: 20px; background: #111; color: #fff; border-radius: 8px;'>"
                                + "<h2 style='color: #ff003c;'>SIÊU BÃO FLASH SALE DÀNH CHO {{customerName}}</h2>"
                                + "<p>Đừng bỏ lỡ ưu đãi giảm giá lên tới 30% cho các sản phẩm Laptop Gaming & Công nghệ cao cấp tuần này!</p>"
                                + "<a href='{{promoUrl}}' style='display: inline-block; padding: 12px 24px; background: #ff003c; color: #fff; text-decoration: none; border-radius: 4px; font-weight: bold;'>Khám Phá Ngay</a>"
                                + "</div>")
                        .variablesJson("[\"customerName\", \"promoUrl\"]")
                        .isActive(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo email templates mặc định: {}", e.getMessage());
        }
    }

    public List<EmailTemplateDTO> getAllTemplates() {
        return templateRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public EmailTemplateDTO getTemplateByCode(String code) {
        return templateRepository.findByCode(code).map(this::mapToDTO).orElse(null);
    }

    @Transactional
    public EmailTemplateDTO saveTemplate(EmailTemplateDTO dto) {
        EmailTemplate template;
        if (dto.getId() != null) {
            template = templateRepository.findById(dto.getId()).orElse(new EmailTemplate());
        } else {
            template = new EmailTemplate();
            template.setCreatedAt(Instant.now());
        }
        template.setCode(dto.getCode());
        template.setName(dto.getName());
        template.setSubjectTemplate(dto.getSubjectTemplate());
        template.setBodyHtml(dto.getBodyHtml());
        template.setVariablesJson(dto.getVariablesJson());
        template.setActive(dto.isActive());
        template.setUpdatedAt(Instant.now());

        EmailTemplate saved = templateRepository.save(template);
        return mapToDTO(saved);
    }

    public String renderContent(String templateHtml, Map<String, String> variables) {
        if (templateHtml == null) return "";
        if (variables == null || variables.isEmpty()) return templateHtml;

        String rendered = templateHtml;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }

    private EmailTemplateDTO mapToDTO(EmailTemplate entity) {
        return EmailTemplateDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .subjectTemplate(entity.getSubjectTemplate())
                .bodyHtml(entity.getBodyHtml())
                .variablesJson(entity.getVariablesJson())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
