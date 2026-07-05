package com.example.doan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaptopRequest {

    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;

    private String price;
    private String link;
    private String images;
    private String brand;
    private String description;

    // CPU
    private String cpuTechnology;
    private String cpuCores;
    private String cpuThreads;
    private String cpuSpeed;
    private String npu;
    private String cpuAiPerformanceTops;

    // GPU
    private String gpuCard;
    private String gpuCores;
    private String gpuTgp;
    private String gpuAiPerformanceTops;

    // RAM
    private String ram;
    private String ramType;
    private String ramBusSpeed;
    private String maxRam;
    private String storage;

    // Screen
    private String screenSize;
    private String screenResolution;
    private String panel;
    private String refreshRate;
    private String colorGamut;
    private String touchScreen;
    private String displayTechnology;

    // I/O
    private String ports;
    private String wireless;
    private String webcam;
    private String keyboardBacklight;
    private String security;
    private String audioTechnology;
    private String cooling;
    private String otherFeatures;
    private String memoryCardReader;

    // Battery & OS
    private String battery;
    private String operatingSystem;
    private String releaseTime;
    private String dimensionsWeight;
    private String material;

    // Inventory
    private Integer stockQuantity;
    private Integer lowStockThreshold;

    private String category;
    private String tag;
    private String shortDescription;
    private String specsJson;
    private String reviewsJson;
    private Double rating;
    private Integer reviewCount;
}