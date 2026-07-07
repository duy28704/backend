package com.example.doan.service;

import com.example.doan.dto.EmailAnalyticsDTO;
import com.example.doan.repository.EmailLogRepository;
import com.example.doan.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailAnalyticsService {

    private final EmailLogRepository logRepository;
    private final EmailQueueRepository queueRepository;

    public EmailAnalyticsDTO getAnalyticsSummary() {
        long successCount = logRepository.countByStatus("SUCCESS");
        long failedCount = logRepository.countByStatus("FAILED");
        long totalSent = successCount + failedCount;
        long pendingQueueCount = queueRepository.countByStatus("PENDING");

        double successRate = totalSent > 0 ? (double) successCount / totalSent * 100 : 100.0;

        // Get daily stats for last 7 days
        Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 3600);
        List<Object[]> rawDailyStats = logRepository.getDailyStatsSince(sevenDaysAgo);
        List<EmailAnalyticsDTO.DailyStat> dailyStats = new ArrayList<>();

        if (rawDailyStats != null) {
            for (Object[] row : rawDailyStats) {
                String dateStr = row[0] != null ? row[0].toString() : LocalDate.now().toString();
                long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long success = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long failed = row[3] != null ? ((Number) row[3]).longValue() : 0L;

                dailyStats.add(new EmailAnalyticsDTO.DailyStat(dateStr, total, success, failed));
            }
        }

        return EmailAnalyticsDTO.builder()
                .totalSent(totalSent)
                .successCount(successCount)
                .failedCount(failedCount)
                .pendingQueueCount(pendingQueueCount)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .dailyStats(dailyStats)
                .build();
    }
}
