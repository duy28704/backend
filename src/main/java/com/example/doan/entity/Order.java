package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id")
    private String id; // format NX-xxxxx

    private String email;
    private String customerName;
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    private String paymentMethod;
    private String paymentCardInfo;
    private String orderDate;
    private String deliveryDate;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String itemsJson;

    private Double subtotal;
    private Double shipping;
    private Double total;

    @Transient
    private String paymentUrl;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
}
