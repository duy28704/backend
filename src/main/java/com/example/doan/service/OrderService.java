package com.example.doan.service;

import com.example.doan.dto.OrderItemDto;
import com.example.doan.entity.Order;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.InventoryTransaction;
import com.example.doan.entity.User;
import com.example.doan.repository.CartItemRepository;
import com.example.doan.repository.OrderRepository;
import com.example.doan.repository.UserRepository;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.repository.InventoryTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final LaptopRepository laptopRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ObjectMapper objectMapper;
    private final VnPayService vnPayService;
    private final NotificationService notificationService;

    @Transactional
    public Order checkout(Order order, HttpServletRequest request) {
        log.info("Bắt đầu xử lý thanh toán đơn hàng cho email người dùng: {}", order.getEmail());
        
        // Generate custom order ID like NX-xxxxx
        String orderId = "NX-" + (10000 + new Random().nextInt(90000));
        order.setId(orderId);
        order.setOrderDate(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        order.setDeliveryDate("Ước tính 2-3 ngày");
        
        if ("vnpay".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setStatus("Chờ thanh toán");
        } else {
            order.setStatus("Chờ xác nhận");
        }

        // Set default masked card details if visa payment is selected
        if ("visa".equalsIgnoreCase(order.getPaymentMethod()) && order.getPaymentCardInfo() == null) {
            order.setPaymentCardInfo("•••• •••• •••• 4242");
        }

        // Chống bán vượt tồn kho & trừ tồn kho
        try {
            List<OrderItemDto> items = objectMapper.readValue(
                    order.getItemsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItemDto.class)
            );

            // Bước 1: Kiểm tra tồn kho của tất cả sản phẩm
            for (OrderItemDto item : items) {
                Laptop laptop = laptopRepository.findById(item.getId())
                        .orElseThrow(() -> {
                            log.error("Thanh toán thất bại: Không tìm thấy sản phẩm với ID {}", item.getId());
                            return new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + item.getId());
                        });

                int stock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
                if (stock < item.getQuantity()) {
                    log.warn("Thanh toán thất bại: Sản phẩm '{}' (ID {}) không đủ tồn kho. Yêu cầu: {}, Hiện có: {}", 
                            laptop.getName(), laptop.getId(), item.getQuantity(), stock);
                    throw new IllegalArgumentException("Sản phẩm '" + laptop.getName() + "' chỉ còn lại " + stock + " sản phẩm trong kho. Vui lòng giảm số lượng đặt hàng!");
                }
            }

            // Bước 2: Thực hiện trừ kho và ghi lịch sử giao dịch
            for (OrderItemDto item : items) {
                Laptop laptop = laptopRepository.findById(item.getId()).get();
                int previousStock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
                int newStock = previousStock - item.getQuantity();

                laptop.setStockQuantity(newStock);
                laptopRepository.save(laptop);
                
                log.info("Đã khấu trừ tồn kho của sản phẩm '{}' (ID {}): {} -> {}", laptop.getName(), laptop.getId(), previousStock, newStock);

                InventoryTransaction transaction = InventoryTransaction.builder()
                        .productId(laptop.getId())
                        .productName(laptop.getName())
                        .type("EXPORT")
                        .quantityChanged(-item.getQuantity())
                        .previousStock(previousStock)
                        .newStock(newStock)
                        .note("Khách hàng đặt hàng, mã đơn: #" + orderId)
                        .createdBy("SYSTEM")
                        .build();

                inventoryTransactionRepository.save(transaction);

                // Cảnh báo tồn kho nếu < 5 sản phẩm
                if (newStock < 5) {
                    notificationService.notifyAllAdmins(
                            "Cảnh báo tồn kho: " + laptop.getName(),
                            "Sản phẩm '" + laptop.getName() + "' (ID " + laptop.getId() + ") chỉ còn " + newStock + " chiếc trong kho!",
                            com.example.doan.entity.NotificationType.INVENTORY,
                            String.valueOf(laptop.getId()),
                            "/admin/products"
                    );
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Có lỗi xảy ra khi kiểm tra tồn kho cho việc thanh toán đơn hàng: ", e);
            throw new RuntimeException("Lỗi xử lý kiểm tra tồn kho đơn hàng: " + e.getMessage(), e);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Tạo đơn hàng thành công: Mã đơn={}, Tổng tiền={}, Số lượng sản phẩm={}", savedOrder.getId(), savedOrder.getTotal(), savedOrder.getItemsJson() != null ? "JSON" : "0");

        // Gửi thông báo cho khách hàng & Admin
        try {
            Optional<User> userOpt = userRepository.findByEmail(order.getEmail());
            String username = userOpt.isPresent() ? userOpt.get().getUsername() : order.getEmail();

            notificationService.createNotification(
                    username,
                    "Đặt hàng thành công #" + savedOrder.getId(),
                    "Đơn hàng #" + savedOrder.getId() + " trị giá " + String.format("%,d", Math.round(savedOrder.getTotal())) + "đ đã được khởi tạo thành công.",
                    com.example.doan.entity.NotificationType.ORDER,
                    savedOrder.getId(),
                    "/profile"
            );

            notificationService.notifyAllAdmins(
                    "Đơn hàng mới #" + savedOrder.getId(),
                    "Khách hàng " + savedOrder.getCustomerName() + " vừa đặt đơn hàng #" + savedOrder.getId() + " trị giá " + String.format("%,d", Math.round(savedOrder.getTotal())) + "đ.",
                    com.example.doan.entity.NotificationType.ORDER,
                    savedOrder.getId(),
                    "/admin/orders"
            );
        } catch (Exception e) {
            log.error("Lỗi khi tạo thông báo đặt hàng: {}", e.getMessage());
        }

        if ("vnpay".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
            String paymentUrl = vnPayService.createPaymentUrl(savedOrder.getId(), savedOrder.getTotal(), request);
            savedOrder.setPaymentUrl(paymentUrl);
        }

        // Clear user's database cart after successful order creation
        try {
            Optional<User> userOpt = userRepository.findByEmail(order.getEmail());
            if (userOpt.isPresent()) {
                cartItemRepository.deleteByUser(userOpt.get());
                log.info("Đã xóa giỏ hàng trong cơ sở dữ liệu cho email người dùng: {}", order.getEmail());
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa giỏ hàng trong cơ sở dữ liệu cho email người dùng: {}", order.getEmail(), e);
        }

        return savedOrder;
    }

    @Transactional
    public void updatePaymentStatus(String orderId, String status) {
        log.info("Cập nhật trạng thái thanh toán cho đơn hàng: {} thành {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy đơn hàng để cập nhật trạng thái: {}", orderId);
                    return new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId);
                });
        order.setStatus(status);
        orderRepository.save(order);
    }

    public List<Order> getHistory(String email) {
        log.info("Đang lấy lịch sử đơn hàng cho email người dùng: {}", email);
        return orderRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }
}
