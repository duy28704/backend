package com.example.doan.service;

import com.example.doan.entity.CartItem;
import com.example.doan.entity.User;

import java.util.List;

public interface CartService {
    List<CartItem> getCart(User user);
    List<CartItem> addToCart(User user, Long productId, Integer quantity, String configuration);
    List<CartItem> updateQuantity(User user, Long productId, String configuration, Integer quantity);
    List<CartItem> removeItem(User user, Long productId, String configuration);
    void clearCart(User user);
}
