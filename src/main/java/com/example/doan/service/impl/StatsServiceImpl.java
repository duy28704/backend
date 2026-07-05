package com.example.doan.service.impl;

import com.example.doan.dto.DashboardStatsDto;
import com.example.doan.dto.OrderItemDto;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.Order;
import com.example.doan.entity.User;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.repository.OrderRepository;
import com.example.doan.repository.UserRepository;
import com.example.doan.repository.SearchLogRepository;
import com.example.doan.service.StatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LaptopRepository laptopRepository;
    private final SearchLogRepository searchLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardStatsDto getDashboardStats() {
        log.info("Bắt đầu tổng hợp thống kê doanh thu và hoạt động hệ thống...");
        
        List<Order> allOrders = orderRepository.findAll();
        List<Laptop> activeLaptops = laptopRepository.findByDeleted(false);
        long totalUsers = userRepository.count();

        java.time.LocalDate localToday = java.time.LocalDate.now();
        Instant todayStart = localToday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthStart = localToday.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant yearStart = localToday.withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        LocalDateTime nowLdt = LocalDateTime.now();
        LocalDateTime todayStartLdt = nowLdt.toLocalDate().atStartOfDay();
        LocalDateTime monthStartLdt = nowLdt.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStartLdt = nowLdt.toLocalDate().withDayOfYear(1).atStartOfDay();

        // 1. Thẻ thống kê (Cards)
        double totalRevenue = allOrders.stream()
                .filter(o -> !"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();
        double todayRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart))
                .filter(o -> !"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();
        double thisMonthRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(monthStart))
                .filter(o -> !"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();
        double thisYearRevenue = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(yearStart))
                .filter(o -> !"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();

        long totalOrdersCount = allOrders.size();
        long todayOrders = allOrders.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart)).count();
        long thisMonthOrders = allOrders.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(monthStart)).count();
        long thisYearOrders = allOrders.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(yearStart)).count();

        List<User> allUsers = userRepository.findAll();
        long newCustomers = allUsers.size();
        long todayCustomers = allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(todayStartLdt)).count();
        long thisMonthCustomers = allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStartLdt)).count();
        long thisYearCustomers = allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(yearStartLdt)).count();

        long totalProducts = activeLaptops.size();
        long todayProducts = activeLaptops.stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(todayStart)).count();
        long thisMonthProducts = activeLaptops.stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(monthStart)).count();
        long thisYearProducts = activeLaptops.stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(yearStart)).count();

        // 2. Line Chart / Area Chart: Doanh thu & Đơn hàng theo ngày
        Map<String, Map<String, Object>> dailyStatsMap = new TreeMap<>();
        for (Order o : allOrders) {
            String date = o.getOrderDate();
            if (date != null && !date.trim().isEmpty()) {
                dailyStatsMap.putIfAbsent(date, new HashMap<>() {{
                    put("date", date);
                    put("revenue", 0.0);
                    put("orders", 0L);
                }});
                Map<String, Object> ds = dailyStatsMap.get(date);
                ds.put("orders", (Long) ds.get("orders") + 1L);
                if (!"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus())) {
                    double t = o.getTotal() != null ? o.getTotal() : 0.0;
                    ds.put("revenue", (Double) ds.get("revenue") + t);
                }
            }
        }
        List<Map<String, Object>> revenueOverTime = new ArrayList<>(dailyStatsMap.values());
        
        revenueOverTime.sort((a, b) -> {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate dateA = java.time.LocalDate.parse((String) a.get("date"), dtf);
                java.time.LocalDate dateB = java.time.LocalDate.parse((String) b.get("date"), dtf);
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0;
            }
        });

        // 3. Horizontal Bar Chart: Top 10 sản phẩm bán chạy
        Map<String, Integer> productSalesMap = new HashMap<>();
        // 5. Pie Chart: Doanh thu theo thương hiệu
        Map<String, Double> brandRevenueMap = new HashMap<>();
        
        Map<Long, String> laptopBrandMap = activeLaptops.stream()
                .filter(l -> l.getId() != null && l.getBrand() != null)
                .collect(Collectors.toMap(Laptop::getId, Laptop::getBrand, (b1, b2) -> b1));

        for (Order o : allOrders) {
            if ("Hủy".equalsIgnoreCase(o.getStatus()) || "Thanh toán thất bại".equalsIgnoreCase(o.getStatus())) continue;
            if (o.getItemsJson() != null && !o.getItemsJson().trim().isEmpty()) {
                try {
                    List<OrderItemDto> items = objectMapper.readValue(
                            o.getItemsJson(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItemDto.class)
                    );
                    for (OrderItemDto item : items) {
                        productSalesMap.put(item.getName(), productSalesMap.getOrDefault(item.getName(), 0) + item.getQuantity());
                        
                        String brand = laptopBrandMap.get(item.getId());
                        if (brand == null) {
                            brand = extractBrandFromName(item.getName());
                        }
                        double itemTotal = (item.getPrice() != null ? item.getPrice() : 0.0) * item.getQuantity();
                        brandRevenueMap.put(brand, brandRevenueMap.getOrDefault(brand, 0.0) + itemTotal);
                    }
                } catch (Exception e) {
                    log.error("Lỗi giải mã JSON sản phẩm trong đơn hàng ID={}", o.getId(), e);
                }
            }
        }

        List<Map<String, Object>> topProducts = productSalesMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("quantity", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> revenueByBrand = brandRevenueMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("brand", entry.getKey());
                    item.put("revenue", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        // 4. Donut Chart: Tỷ lệ đơn hàng theo trạng thái
        Map<String, Long> statusCountMap = allOrders.stream()
                .collect(Collectors.groupingBy(o -> {
                    String s = o.getStatus();
                    if (s == null) return "Chờ xác nhận";
                    return s;
                }, Collectors.counting()));
        
        List<Map<String, Object>> ordersByStatus = statusCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("status", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        // 6. Mixed Chart: So sánh doanh thu và số lượng đơn hàng theo từng tháng
        Map<String, Map<String, Object>> monthlyStatsMap = new TreeMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Order o : allOrders) {
            Instant ins = o.getCreatedAt();
            if (ins == null) ins = Instant.now();
            LocalDateTime ldt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
            String month = ldt.format(monthFormatter);
            
            monthlyStatsMap.putIfAbsent(month, new HashMap<>() {{
                put("month", month);
                put("revenue", 0.0);
                put("orders", 0L);
            }});
            
            Map<String, Object> stats = monthlyStatsMap.get(month);
            stats.put("orders", (Long) stats.get("orders") + 1L);
            if (!"Hủy".equalsIgnoreCase(o.getStatus()) && !"Thanh toán thất bại".equalsIgnoreCase(o.getStatus())) {
                double t = o.getTotal() != null ? o.getTotal() : 0.0;
                stats.put("revenue", (Double) stats.get("revenue") + t);
            }
        }
        List<Map<String, Object>> monthlyMixedStats = new ArrayList<>(monthlyStatsMap.values());

        // 7. Heatmap: Khung giờ hoặc ngày trong tuần có nhiều đơn hàng nhất
        String[] daysOfWeek = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
        Map<String, Map<Integer, Integer>> heatmapGrid = new HashMap<>();
        for (String day : daysOfWeek) {
            heatmapGrid.put(day, new HashMap<>());
            for (int h = 0; h < 24; h++) {
                heatmapGrid.get(day).put(h, 0);
            }
        }

        for (Order o : allOrders) {
            Instant ins = o.getCreatedAt();
            if (ins == null) continue;
            LocalDateTime ldt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
            int dayIndex = ldt.getDayOfWeek().getValue() % 7;
            String dayName = daysOfWeek[dayIndex];
            int hour = ldt.getHour();
            
            Map<Integer, Integer> hourMap = heatmapGrid.get(dayName);
            hourMap.put(hour, hourMap.get(hour) + 1);
        }

        List<Map<String, Object>> heatmapStats = new ArrayList<>();
        for (String day : daysOfWeek) {
            for (int h = 0; h < 24; h++) {
                int count = heatmapGrid.get(day).get(h);
                if (count > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("day", day);
                    item.put("hour", h);
                    item.put("count", count);
                    heatmapStats.add(item);
                }
            }
        }

        List<Map<String, Object>> visitsData = searchLogRepository.findSearchVolumeOverTime();
        List<Map<String, Object>> websiteVisitsOverTime = visitsData.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", m.get("searchDate") != null ? m.get("searchDate").toString() : "");
            item.put("visits", m.get("searchCount") != null ? m.get("searchCount") : 0L);
            return item;
        }).collect(Collectors.toList());

        return DashboardStatsDto.builder()
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .thisYearRevenue(thisYearRevenue)
                .totalOrders(totalOrdersCount)
                .todayOrders(todayOrders)
                .thisMonthOrders(thisMonthOrders)
                .thisYearOrders(thisYearOrders)
                .newCustomers(newCustomers)
                .todayCustomers(todayCustomers)
                .thisMonthCustomers(thisMonthCustomers)
                .thisYearCustomers(thisYearCustomers)
                .totalProducts(totalProducts)
                .todayProducts(todayProducts)
                .thisMonthProducts(thisMonthProducts)
                .thisYearProducts(thisYearProducts)
                .revenueOverTime(revenueOverTime)
                .topProducts(topProducts)
                .ordersByStatus(ordersByStatus)
                .revenueByBrand(revenueByBrand)
                .monthlyMixedStats(monthlyMixedStats)
                .heatmapStats(heatmapStats)
                .websiteVisitsOverTime(websiteVisitsOverTime)
                .build();
    }

    private String extractBrandFromName(String name) {
        if (name == null) return "Other";
        String lower = name.toLowerCase();
        if (lower.contains("dell")) return "Dell";
        if (lower.contains("asus")) return "ASUS";
        if (lower.contains("lenovo")) return "Lenovo";
        if (lower.contains("hp")) return "HP";
        if (lower.contains("msi")) return "MSI";
        if (lower.contains("acer")) return "Acer";
        if (lower.contains("macbook") || lower.contains("apple")) return "Apple";
        if (lower.contains("gigabyte")) return "Gigabyte";
        return "Other";
    }
}
