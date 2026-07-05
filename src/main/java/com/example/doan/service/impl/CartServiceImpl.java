package com.example.doan.service.impl;

import com.example.doan.entity.CartItem;
import com.example.doan.entity.Product;
import com.example.doan.entity.User;
import com.example.doan.repository.CartItemRepository;
import com.example.doan.repository.ProductRepository;
import com.example.doan.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CartItem> getCart(User user) {
        log.info("Lấy thông tin giỏ hàng của người dùng: {}", user.getEmail());
        return cartItemRepository.findByUser(user);
    }

    @Override
    @Transactional
    public List<CartItem> addToCart(User user, Long productId, Integer quantity, String configuration) {
        log.info("Thêm sản phẩm vào giỏ hàng cho người dùng '{}': ID sản phẩm={}, số lượng={}, cấu hình='{}'", 
                user.getEmail(), productId, quantity, configuration);
                
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm ID={} khi thêm vào giỏ hàng của người dùng '{}'", productId, user.getEmail());
                    return new RuntimeException("Product not found with id: " + productId);
                });

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByUserAndProductIdAndConfiguration(user, productId, configuration);

        Double basePrice = parsePrice(product.getPrice());
        Double finalPrice = getPriceForConfig(basePrice, configuration);

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(finalPrice);
            existingItem.setSubtotal(finalPrice * newQuantity);
            cartItemRepository.save(existingItem);
            log.info("Đã cập nhật số lượng cho sản phẩm ID={} trong giỏ hàng: số lượng mới={}", productId, newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(quantity)
                    .configuration(configuration)
                    .price(finalPrice)
                    .subtotal(finalPrice * quantity)
                    .build();
            cartItemRepository.save(newItem);
            log.info("Đã tạo mới vật phẩm sản phẩm ID={} trong giỏ hàng", productId);
        }

        return getCart(user);
    }

    @Override
    @Transactional
    public List<CartItem> updateQuantity(User user, Long productId, String configuration, Integer quantity) {
        log.info("Cập nhật số lượng sản phẩm ID={} trong giỏ hàng của '{}' thành: {} (cấu hình='{}')", 
                productId, user.getEmail(), quantity, configuration);
                
        Optional<CartItem> itemOpt = cartItemRepository
                .findByUserAndProductIdAndConfiguration(user, productId, configuration);

        if (itemOpt.isPresent()) {
            CartItem item = itemOpt.get();
            if (quantity <= 0) {
                cartItemRepository.delete(item);
                log.info("Xóa hoàn toàn sản phẩm ID={} ra khỏi giỏ hàng do số lượng <= 0", productId);
            } else {
                item.setQuantity(quantity);
                Double basePrice = parsePrice(item.getProduct().getPrice());
                Double finalPrice = getPriceForConfig(basePrice, configuration);
                item.setPrice(finalPrice);
                item.setSubtotal(finalPrice * quantity);
                cartItemRepository.save(item);
                log.info("Đã cập nhật số lượng mới trong giỏ hàng thành công");
            }
        } else {
            log.warn("Không tìm thấy sản phẩm ID={} với cấu hình '{}' trong giỏ hàng của người dùng '{}' để cập nhật", 
                    productId, configuration, user.getEmail());
        }

        return getCart(user);
    }

    @Override
    @Transactional
    public List<CartItem> removeItem(User user, Long productId, String configuration) {
        log.info("Xóa sản phẩm ID={} cấu hình '{}' khỏi giỏ hàng của người dùng: {}", productId, configuration, user.getEmail());
        cartItemRepository.deleteByUserAndProductIdAndConfiguration(user, productId, configuration);
        return getCart(user);
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        log.info("Xóa sạch toàn bộ giỏ hàng của người dùng: {}", user.getEmail());
        cartItemRepository.deleteByUser(user);
    }

    private Double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return 0.0;
        }
        String str = priceStr.trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9.,]+)\\s*₫");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            String cleaned = matcher.group(1).replaceAll("[^0-9]", "");
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        java.util.regex.Pattern digitPattern = java.util.regex.Pattern.compile("[0-9.]+");
        java.util.regex.Matcher digitMatcher = digitPattern.matcher(str);
        if (digitMatcher.find()) {
            String cleaned = digitMatcher.group().replaceAll("[^0-9]", "");
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 0.0;
    }

    private Double getPriceForConfig(Double basePrice, String configuration) {
        if (configuration != null && configuration.contains("Hiệu năng cao")) {
            return basePrice + 2500000.0;
        }
        return basePrice;
    }
}
