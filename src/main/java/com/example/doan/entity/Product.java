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
    public Long id;

    @Column(nullable = false, length = 1000)
    public String name;

    @Column(length = 1000)
    public String price;

    @Column(length = 1500, unique = true)
    public String link;

    @Column(columnDefinition = "CLOB")
    public String images;

    public String brand;

    @Column(length = 255)
    public String category;

    public Double rating;

    public Integer reviewCount;

    @Column(columnDefinition = "CLOB")
    public String shortDescription;

    @Column(length = 255)
    public String tag;

    @Column(columnDefinition = "CLOB")
    public String specsJson;

    @Column(columnDefinition = "CLOB")
    public String reviewsJson;

    @Column(columnDefinition = "CLOB")
    public String description;

    @Column(nullable = false)
    public boolean deleted = false;

    public Instant deletedAt;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}