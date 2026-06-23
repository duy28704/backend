package com.example.doan.repository;

import com.example.doan.entity.CartItem;
import com.example.doan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    
    Optional<CartItem> findByUserAndProductIdAndConfiguration(User user, Long productId, String configuration);
    
    void deleteByUserAndProductIdAndConfiguration(User user, Long productId, String configuration);
    
    void deleteByUser(User user);
}
