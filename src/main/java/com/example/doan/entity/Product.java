package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "products")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false, length = 1000)
    private String name;

    private Double price;

    @Column(length = 1500, unique = true)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String images;

    private String brand;

    @Column(length = 255)
    private String category;

    private Double rating;

    private Integer reviewCount;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(length = 255)
    private String tag;

    @Column(columnDefinition = "TEXT")
    private String specsJson;

    @Column(columnDefinition = "TEXT")
    private String reviewsJson;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    private Instant deletedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}