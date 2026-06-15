package com.example.doan.dto;


import com.example.doan.excel.ExcelColumn;
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

    @ExcelColumn(value = "Tên sản phẩm", required = true)
    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;

    @ExcelColumn("Giá")
    private String price;

    @ExcelColumn("Link")
    private String link;

    @ExcelColumn("Ảnh")
    private String images;

    @ExcelColumn("Hãng")
    private String brand;

    @ExcelColumn("Mô tả")
    private String description;

    // CPU
    @ExcelColumn("CPU - Công nghệ")
    private String cpuTechnology;

    @ExcelColumn("CPU - Số nhân")
    private Integer cpuCores;

    @ExcelColumn("CPU - Số luồng")
    private Integer cpuThreads;

    @ExcelColumn("CPU - Tốc độ")
    private String cpuSpeed;

    @ExcelColumn("CPU - NPU")
    private String npu;

    @ExcelColumn("CPU - AI TOPS")
    private Double cpuAiPerformanceTops;

    // GPU
    @ExcelColumn("GPU - Card")
    private String gpuCard;

    @ExcelColumn("GPU - Số nhân")
    private Integer gpuCores;

    @ExcelColumn("GPU - TGP")
    private Integer gpuTgp;

    @ExcelColumn("GPU - AI TOPS")
    private Double gpuAiPerformanceTops;

    // RAM
    @ExcelColumn("RAM")
    private String ram;

    @ExcelColumn("RAM - Loại")
    private String ramType;

    @ExcelColumn("RAM - Bus")
    private Integer ramBusSpeed;

    @ExcelColumn("RAM - Tối đa")
    private String maxRam;

    @ExcelColumn("Ổ cứng")
    private String storage;

    // Screen
    @ExcelColumn("Màn hình - Size")
    private String screenSize;

    @ExcelColumn("Màn hình - Độ phân giải")
    private String screenResolution;

    @ExcelColumn("Màn hình - Panel")
    private String panel;

    @ExcelColumn("Màn hình - Tần số quét")
    private Integer refreshRate;

    @ExcelColumn("Màn hình - Màu sắc")
    private String colorGamut;

    @ExcelColumn("Màn hình - Cảm ứng")
    private String touchScreen;

    @ExcelColumn("Màn hình - Công nghệ")
    private String displayTechnology;

    // I/O
    @ExcelColumn("Cổng")
    private String ports;

    @ExcelColumn("Wireless")
    private String wireless;

    @ExcelColumn("Webcam")
    private String webcam;

    @ExcelColumn("Keyboard Backlight")
    private String keyboardBacklight;

    @ExcelColumn("Security")
    private String security;

    @ExcelColumn("Audio")
    private String audioTechnology;

    @ExcelColumn("Cooling")
    private String cooling;

    @ExcelColumn("Features")
    private String otherFeatures;

    // Battery & OS
    @ExcelColumn("Pin")
    private String battery;

    @ExcelColumn("Hệ điều hành")
    private String operatingSystem;

    @ExcelColumn("Ngày ra mắt")
    private String releaseTime;

    @ExcelColumn("Kích thước")
    private String dimensionsWeight;

    @ExcelColumn("Chất liệu")
    private String material;

    @ExcelColumn("Thẻ nhớ")
    private String memoryCardReader;

    private String category;
    private String tag;
    private String shortDescription;
    private String specsJson;
    private String reviewsJson;
    private Double rating;
    private Integer reviewCount;
}