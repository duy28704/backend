package com.example.doan.repository;

import com.example.doan.entity.CartItem;
import com.example.doan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    
    Optional<CartItem> findByUserAndProductIdAndConfiguration(User user, Long productId, String configuration);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.user = ?1 AND c.product.id = ?2 AND c.configuration = ?3")
    void deleteByUserAndProductIdAndConfiguration(User user, Long productId, String configuration);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.user = ?1")
    void deleteByUser(User user);
}
