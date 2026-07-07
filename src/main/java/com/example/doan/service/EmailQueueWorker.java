package com.example.doan.service;

import com.example.doan.entity.EmailLog;
import com.example.doan.entity.EmailQueue;
import com.example.doan.repository.EmailLogRepository;
import com.example.doan.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class EmailQueueWorker {

    private final EmailQueueRepository queueRepository;
    private final EmailLogRepository logRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Scheduled(fixedDelay = 5000) // Runs every 5 seconds
    @Transactional
    public void processEmailQueue() {
        Instant now = Instant.now();
        List<EmailQueue> queueItems = queueRepository.findNextBatchToProcess(now, PageRequest.of(0, 20));

        if (queueItems.isEmpty()) {
            return;
        }

        log.info("EmailQueueWorker đang xử lý {} email trong hàng đợi...", queueItems.size());

        for (EmailQueue item : queueItems) {
            item.setStatus("PROCESSING");
            queueRepository.save(item);

            boolean success = false;
            String errorMsg = null;

            try {
                if (mailSender != null) {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                    helper.setTo(item.getRecipientEmail());
                    helper.setSubject(item.getSubject());
                    helper.setText(item.getBodyHtml(), true);

                    mailSender.send(mimeMessage);
                    success = true;
                    log.info("EmailQueueWorker gửi email thành công tới {}", item.getRecipientEmail());
                } else {
                    log.warn("JavaMailSender chưa cấu hình SMTP. Giả lập gửi thành công tới {}", item.getRecipientEmail());
                    success = true;
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                log.error("EmailQueueWorker gửi lỗi tới {}: {}", item.getRecipientEmail(), errorMsg);
            }

            if (success) {
                item.setStatus("SENT");
                item.setSentAt(Instant.now());
                queueRepository.save(item);

                logRepository.save(EmailLog.builder()
                        .queueId(item.getId())
                        .recipientEmail(item.getRecipientEmail())
                        .subject(item.getSubject())
                        .templateCode(item.getTemplateCode())
                        .status("SUCCESS")
                        .sentAt(Instant.now())
                        .build());
            } else {
                int newRetry = item.getRetryCount() + 1;
                item.setRetryCount(newRetry);
                item.setErrorMessage(errorMsg);
                if (newRetry >= item.getMaxRetries()) {
                    item.setStatus("FAILED");
                } else {
                    item.setStatus("PENDING");
                    item.setScheduledAt(Instant.now().plusSeconds(newRetry * 120L)); // Delay exponential
                }
                queueRepository.save(item);

                logRepository.save(EmailLog.builder()
                        .queueId(item.getId())
                        .recipientEmail(item.getRecipientEmail())
                        .subject(item.getSubject())
                        .templateCode(item.getTemplateCode())
                        .status("FAILED")
                        .errorDetails(errorMsg)
                        .sentAt(Instant.now())
                        .build());
            }
        }
    }
}
