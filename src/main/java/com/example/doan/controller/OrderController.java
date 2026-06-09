package com.example.doan.controller;

import com.example.doan.entity.Order;
import com.example.doan.repository.OrderRepository;
import com.example.doan.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Order>> checkout(@RequestBody Order order) {
        // Generate custom order ID like NX-xxxxx
        String orderId = "NX-" + (10000 + new Random().nextInt(90000));
        order.setId(orderId);
        order.setOrderDate(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        order.setDeliveryDate("Ước tính 2-3 ngày");
        order.setStatus("Chờ xác nhận");

        // Set default masked card details if visa payment is selected
        if ("visa".equalsIgnoreCase(order.getPaymentMethod()) && order.getPaymentCardInfo() == null) {
            order.setPaymentCardInfo("•••• •••• •••• 4242");
        }

        Order savedOrder = orderRepository.save(order);

        ApiResponse<Order> response = ApiResponse.<Order>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Checkout success")
                .data(savedOrder)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Order>>> getHistory(@RequestParam String email) {
        List<Order> orders = orderRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email);

        ApiResponse<List<Order>> response = ApiResponse.<List<Order>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get order history success")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }
}
