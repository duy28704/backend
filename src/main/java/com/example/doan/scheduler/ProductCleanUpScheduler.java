package com.example.doan.scheduler;

import com.example.doan.entity.Laptop;
import com.example.doan.repository.LaptopRepository;
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

    // Run every day at midnight: "0 0 0 * * *"
    // For testing and visibility, we also log a message during execution
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanUpDeletedProducts() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant twentySevenDaysAgo = now.minus(27, ChronoUnit.DAYS);

        List<Laptop> deletedLaptops = laptopRepository.findByDeleted(true);
        log.info("[Scheduler] Checking for soft-deleted products to cleanup. Total deleted items: {}", deletedLaptops.size());

        for (Laptop laptop : deletedLaptops) {
            Instant deletedAt = laptop.getDeletedAt();
            if (deletedAt == null) continue;

            if (deletedAt.isBefore(thirtyDaysAgo)) {
                // Hard delete
                laptopRepository.delete(laptop);
                log.info("[Scheduler] Automatically permanently deleted expired product: {} (ID: {})", laptop.getName(), laptop.getId());
            } else if (deletedAt.isBefore(twentySevenDaysAgo)) {
                // Impending deletion warning (3 days or fewer remaining)
                long daysRemaining = 30 - ChronoUnit.DAYS.between(deletedAt, now);
                log.warn("[Scheduler Warning] Product '{}' (ID: {}) will be permanently deleted in {} days if not restored!",
                        laptop.getName(), laptop.getId(), daysRemaining);
            }
        }
    }
}
