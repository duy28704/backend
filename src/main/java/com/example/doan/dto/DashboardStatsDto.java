package com.example.doan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private Double totalRevenue;
    private Double todayRevenue;
    private Double thisMonthRevenue;
    private Double thisYearRevenue;

    private Long totalOrders;
    private Long todayOrders;
    private Long thisMonthOrders;
    private Long thisYearOrders;

    private Long newCustomers; // Total customers count (historical)
    private Long todayCustomers;
    private Long thisMonthCustomers;
    private Long thisYearCustomers;

    private Long totalProducts; // Active catalog count
    private Long todayProducts;
    private Long thisMonthProducts;
    private Long thisYearProducts;
    private List<Map<String, Object>> revenueOverTime;
    private List<Map<String, Object>> topProducts;
    private List<Map<String, Object>> ordersByStatus;
    private List<Map<String, Object>> revenueByBrand;
    private List<Map<String, Object>> monthlyMixedStats;
    private List<Map<String, Object>> heatmapStats;
    private List<Map<String, Object>> websiteVisitsOverTime;
}
