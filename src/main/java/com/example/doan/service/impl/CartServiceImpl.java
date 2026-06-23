package com.example.doan.service.impl;

import com.example.doan.entity.CartItem;
import com.example.doan.entity.Product;
import com.example.doan.entity.User;
import com.example.doan.repository.CartItemRepository;
import com.example.doan.repository.ProductRepository;
import com.example.doan.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CartItem> getCart(User user) {
        return cartItemRepository.findByUser(user);
    }

    @Override
    @Transactional
    public List<CartItem> addToCart(User user, Long productId, Integer quantity, String configuration) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByUserAndProductIdAndConfiguration(user, productId, configuration);

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(quantity)
                    .configuration(configuration)
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCart(user);
    }

    @Override
    @Transactional
    public List<CartItem> updateQuantity(User user, Long productId, String configuration, Integer quantity) {
        Optional<CartItem> itemOpt = cartItemRepository
                .findByUserAndProductIdAndConfiguration(user, productId, configuration);

        if (itemOpt.isPresent()) {
            CartItem item = itemOpt.get();
            if (quantity <= 0) {
                cartItemRepository.delete(item);
            } else {
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            }
        }

        return getCart(user);
    }

    @Override
    @Transactional
    public List<CartItem> removeItem(User user, Long productId, String configuration) {
        cartItemRepository.deleteByUserAndProductIdAndConfiguration(user, productId, configuration);
        return getCart(user);
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }
}
