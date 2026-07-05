package com.example.doan.service;

import com.example.doan.dto.InventoryRequest;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.InventoryTransaction;
import com.example.doan.entity.User;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final LaptopRepository laptopRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public InventoryTransaction createTransaction(User user, InventoryRequest request) {
        String creatorEmail = (user != null) ? user.getEmail() : "ADMIN";
        log.info("Bắt đầu tạo giao dịch kho: loại={}, sản phẩm ID={}, số lượng={}, yêu cầu bởi={}", 
                request.getType(), request.getProductId(), request.getQuantity(), creatorEmail);

        Laptop laptop = laptopRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Giao dịch kho thất bại: Không tìm thấy sản phẩm với ID={}", request.getProductId());
                    return new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + request.getProductId());
                });

        int previousStock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
        int newStock = previousStock;
        int quantityChanged = 0;

        String type = request.getType().toUpperCase();
        switch (type) {
            case "IMPORT":
                if (request.getQuantity() <= 0) {
                    log.warn("Giao dịch kho thất bại: Số lượng nhập phải là số dương. Nhận được={}", request.getQuantity());
                    throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
                }
                newStock = previousStock + request.getQuantity();
                quantityChanged = request.getQuantity();
                break;
            case "EXPORT":
                if (request.getQuantity() <= 0) {
                    log.warn("Giao dịch kho thất bại: Số lượng xuất phải là số dương. Nhận được={}", request.getQuantity());
                    throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0");
                }
                if (previousStock < request.getQuantity()) {
                    log.warn("Giao dịch kho thất bại: Không đủ tồn kho để xuất. Hiện có={}, Yêu cầu={}", previousStock, request.getQuantity());
                    throw new IllegalArgumentException("Không đủ tồn kho để xuất. Tồn hiện tại: " + previousStock);
                }
                newStock = previousStock - request.getQuantity();
                quantityChanged = -request.getQuantity();
                break;
            case "ADJUST":
                newStock = previousStock + request.getQuantity();
                quantityChanged = request.getQuantity();
                if (newStock < 0) {
                    log.warn("Giao dịch kho thất bại: Số lượng tồn kho sau điều chỉnh không thể âm. Hiện có={}, Điều chỉnh={}", previousStock, request.getQuantity());
                    throw new IllegalArgumentException("Tồn kho sau điều chỉnh không thể nhỏ hơn 0. Tồn hiện tại: " + previousStock);
                }
                break;
            case "AUDIT":
                if (request.getQuantity() < 0) {
                    log.warn("Giao dịch kho thất bại: Số lượng kiểm kê thực tế không thể âm. Nhận được={}", request.getQuantity());
                    throw new IllegalArgumentException("Số lượng kiểm kê thực tế không thể nhỏ hơn 0");
                }
                newStock = request.getQuantity();
                quantityChanged = newStock - previousStock;
                break;
            default:
                log.warn("Giao dịch kho thất bại: Loại nghiệp vụ không hợp lệ: {}", request.getType());
                throw new IllegalArgumentException("Loại nghiệp vụ không hợp lệ: " + request.getType());
        }

        // Update product stock
        laptop.setStockQuantity(newStock);
        laptopRepository.save(laptop);

        // Record transaction log
        InventoryTransaction transaction = InventoryTransaction.builder()
                .productId(laptop.getId())
                .productName(laptop.getName())
                .type(type)
                .quantityChanged(quantityChanged)
                .previousStock(previousStock)
                .newStock(newStock)
                .note(request.getNote())
                .createdBy(creatorEmail)
                .build();

        InventoryTransaction savedTx = inventoryTransactionRepository.save(transaction);
        log.info("Giao dịch kho đã được ghi nhận thành công: Mã GD={}, loại={}, sản phẩm='{}', thay đổi số lượng={} ({} -> {})", 
                savedTx.getId(), type, laptop.getName(), quantityChanged, previousStock, newStock);

        return savedTx;
    }

    public List<InventoryTransaction> getAllTransactions() {
        log.info("Đang lấy toàn bộ lịch sử giao dịch kho");
        return inventoryTransactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<InventoryTransaction> getTransactionsByProduct(Long productId) {
        log.info("Đang lấy lịch sử giao dịch kho của sản phẩm ID={}", productId);
        return inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}

