package com.example.doan.controller;

import com.example.doan.dto.InventoryRequest;
import com.example.doan.entity.InventoryTransaction;
import com.example.doan.entity.User;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/transaction")
    public ResponseEntity<ApiResponse<InventoryTransaction>> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InventoryRequest request
    ) {
        InventoryTransaction transaction = inventoryService.createTransaction(user, request);

        ApiResponse<InventoryTransaction> response = ApiResponse.<InventoryTransaction>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Tạo giao dịch kho thành công")
                .data(transaction)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getAllTransactions() {
        List<InventoryTransaction> transactions = inventoryService.getAllTransactions();

        ApiResponse<List<InventoryTransaction>> response = ApiResponse.<List<InventoryTransaction>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách giao dịch kho thành công")
                .data(transactions)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getTransactionsByProduct(@PathVariable Long productId) {
        List<InventoryTransaction> transactions = inventoryService.getTransactionsByProduct(productId);

        ApiResponse<List<InventoryTransaction>> response = ApiResponse.<List<InventoryTransaction>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Lấy giao dịch kho theo sản phẩm thành công")
                .data(transactions)
                .build();

        return ResponseEntity.ok(response);
    }
}
