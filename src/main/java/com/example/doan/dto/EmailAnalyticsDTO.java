package com.example.doan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAnalyticsDTO {
    private long totalSent;
    private long successCount;
    private long failedCount;
    private long pendingQueueCount;
    private double successRate;
    private List<DailyStat> dailyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String date;
        private long total;
        private long success;
        private long failed;
    }
}
