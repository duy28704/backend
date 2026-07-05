package com.example.doan.controller;

import com.example.doan.dto.DashboardStatsDto;
import com.example.doan.entity.Order;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.OrderService;
import com.example.doan.service.StatsService;
import com.example.doan.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.doan.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final VnPayService vnPayService;
    private final StatsService statsService;

    @Value("${vnpay.frontend-url}")
    private String frontendUrl;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Order>> checkout(@RequestBody Order order, HttpServletRequest request) {
        try {
            Order savedOrder = orderService.checkout(order, request);

            ApiResponse<Order> response = ApiResponse.<Order>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CREATED.value())
                    .message("Checkout success")
                    .data(savedOrder)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            ApiResponse<Order> response = ApiResponse.<Order>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            ApiResponse<Order> response = ApiResponse.<Order>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(e.getMessage())
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<Void> vnpayCallback(@RequestParam Map<String, String> queryParams) {
        log.info("Nhận callback thanh toán từ VNPAY: {}", queryParams);
        
        // Map is immutable or has security fields, we need to create a mutable copy to verify signature
        Map<String, String> fields = new HashMap<>(queryParams);
        boolean isValid = vnPayService.verifySignature(fields);
        
        String orderId = queryParams.get("vnp_TxnRef");
        String responseCode = queryParams.get("vnp_ResponseCode");
        
        String redirectUrl = frontendUrl + "/#shop?vnpay=fail";
        if (isValid) {
            if ("00".equals(responseCode)) {
                orderService.updatePaymentStatus(orderId, "Đã thanh toán");
                log.info("Thanh toán VNPAY thành công cho đơn hàng: {}", orderId);
                redirectUrl = frontendUrl + "/#shop?vnpay=success&orderId=" + orderId;
            } else {
                orderService.updatePaymentStatus(orderId, "Thanh toán thất bại");
                log.warn("Thanh toán VNPAY thất bại cho đơn hàng: {}, Mã phản hồi: {}", orderId, responseCode);
                redirectUrl = frontendUrl + "/#shop?vnpay=fail&orderId=" + orderId + "&errorCode=" + responseCode;
            }
        } else {
            log.error("Xác thực chữ ký VNPAY thất bại cho đơn hàng: {}", orderId);
            redirectUrl = frontendUrl + "/#shop?vnpay=error&orderId=" + orderId;
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Order>>> getHistory(
            @AuthenticationPrincipal User user,
            @RequestParam String email
    ) {
        boolean canViewOthersHistory = user != null && user.getAuthorities().stream()
                .anyMatch(a -> "order.manage".equals(a.getAuthority()));
        if (user != null && !user.getEmail().equalsIgnoreCase(email) && !canViewOthersHistory) {
            throw new RuntimeException("Bạn không có quyền truy cập dữ liệu lịch sử đơn hàng của tài khoản khác.");
        }

        List<Order> orders = orderService.getHistory(email);

        ApiResponse<List<Order>> response = ApiResponse.<List<Order>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get order history success")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = statsService.getDashboardStats();
        ApiResponse<DashboardStatsDto> response = ApiResponse.<DashboardStatsDto>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get dashboard stats success")
                .data(stats)
                .build();
        return ResponseEntity.ok(response);
    }
}
