package com.example.doan.scheduler;

import com.example.doan.entity.Laptop;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCleanUpScheduler {

    private final LaptopRepository laptopRepository;
    private final ProductService productService;

    // Run every day at midnight: "0 0 0 * * *"
    // For testing and visibility, we also log a message during execution
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanUpDeletedProducts() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant twentySevenDaysAgo = now.minus(27, ChronoUnit.DAYS);

        List<Laptop> deletedLaptops = laptopRepository.findByDeleted(true);
        log.info("[Bộ lập lịch] Đang kiểm tra các sản phẩm đã bị xóa tạm thời để dọn dẹp. Tổng số lượng: {}", deletedLaptops.size());

        for (Laptop laptop : deletedLaptops) {
            Instant deletedAt = laptop.getDeletedAt();
            if (deletedAt == null) continue;

            if (deletedAt.isBefore(thirtyDaysAgo)) {
                // Hard delete
                productService.hardDeleteLaptop(laptop.getId());
                log.info("[Bộ lập lịch] Tự động xóa vĩnh viễn sản phẩm đã hết hạn khôi phục: {} (ID: {})", laptop.getName(), laptop.getId());
            } else if (deletedAt.isBefore(twentySevenDaysAgo)) {
                // Impending deletion warning (3 days or fewer remaining)
                long daysRemaining = 30 - ChronoUnit.DAYS.between(deletedAt, now);
                log.warn("[Cảnh báo Bộ lập lịch] Sản phẩm '{}' (ID: {}) sẽ bị xóa vĩnh viễn sau {} ngày nữa nếu không được khôi phục!",
                        laptop.getName(), laptop.getId(), daysRemaining);
            }
        }
    }
}
