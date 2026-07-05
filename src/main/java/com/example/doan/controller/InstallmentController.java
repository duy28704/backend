package com.example.doan.controller;

import com.example.doan.entity.Installment;
import com.example.doan.repository.InstallmentRepository;
import com.example.doan.response.ApiResponse;
import com.example.doan.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/installments")
@RequiredArgsConstructor
public class InstallmentController {

    private final InstallmentRepository installmentRepository;

    @PostMapping("/submit-request")
    public ResponseEntity<ApiResponse<Installment>> submitRequest(@RequestBody Installment installment) {
        // Generate custom ID like INS-xxxxx
        String installmentId = "INS-" + (10000 + new Random().nextInt(90000));
        installment.setId(installmentId);
        installment.setCreatedDate(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        installment.setStatus("Chờ duyệt");

        Installment savedInstallment = installmentRepository.save(installment);

        ApiResponse<Installment> response = ApiResponse.<Installment>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Submit installment request success")
                .data(savedInstallment)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<Installment>>> getRequests(
            @AuthenticationPrincipal User user,
            @RequestParam String email
    ) {
        boolean canViewOthersInstallments = user != null && user.getAuthorities().stream()
                .anyMatch(a -> "stats.view".equals(a.getAuthority()));
        if (user != null && !user.getEmail().equalsIgnoreCase(email) && !canViewOthersInstallments) {
            throw new RuntimeException("Bạn không có quyền truy cập dữ liệu đăng ký trả góp của tài khoản khác.");
        }

        List<Installment> requests = installmentRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email);

        ApiResponse<List<Installment>> response = ApiResponse.<List<Installment>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get installment requests success")
                .data(requests)
                .build();

        return ResponseEntity.ok(response);
    }
}
