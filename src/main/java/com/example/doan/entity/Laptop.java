package com.example.doan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "laptops")
@PrimaryKeyJoinColumn(name = "product_id")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Laptop extends Product {

    private String cpuTechnology;
    private String cpuCores;
    private String cpuThreads;
    private String cpuSpeed;
    private String npu;
    private String cpuAiPerformanceTops;

    private String gpuCard;
    private String gpuCores;
    private String gpuTgp;
    private String gpuAiPerformanceTops;

    private String ram;
    private String ramType;
    private String ramBusSpeed;
    private String maxRam;
    private String storage;

    private String screenSize;
    private String screenResolution;
    private String panel;
    private String refreshRate;
    private String colorGamut;
    private String touchScreen;

    @Column(columnDefinition = "TEXT")
    private String displayTechnology;

    @Column(columnDefinition = "TEXT")
    private String ports;

    @Column(columnDefinition = "TEXT")
    private String wireless;

    private String webcam;
    private String keyboardBacklight;

    @Column(columnDefinition = "TEXT")
    private String security;

    @Column(columnDefinition = "TEXT")
    private String audioTechnology;

    private String cooling;

    @Column(columnDefinition = "TEXT")
    private String otherFeatures;

    private String battery;
    private String operatingSystem;
    private String releaseTime;
    private String dimensionsWeight;
    private String material;
    private String memoryCardReader;

    // getter/setter
}
