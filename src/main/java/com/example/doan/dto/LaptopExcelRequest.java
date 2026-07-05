package com.example.doan.dto;

import com.example.doan.excel.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaptopExcelRequest {

    @ExcelColumn(value = "Tên sản phẩm", required = true)
    private String name;

    @ExcelColumn("Giá")
    private String price;

    @ExcelColumn("Link")
    private String link;

    @ExcelColumn("Ảnh")
    private String images;

    @ExcelColumn("Kích thước - Khối lượng - Pin - Hãng")
    private String brand;

    @ExcelColumn("Mô tả")
    private String description;

    // CPU
    @ExcelColumn("CPU - Công nghệ CPU")
    private String cpuTechnology;

    @ExcelColumn("CPU - Số nhân")
    private String cpuCores;

    @ExcelColumn("CPU - Số luồng")
    private String cpuThreads;

    @ExcelColumn("CPU - Tốc độ CPU")
    private String cpuSpeed;

    @ExcelColumn("CPU - NPU")
    private String npu;

    @ExcelColumn("CPU - Hiệu năng xử lý AI (TOPS)")
    private String cpuAiPerformanceTops;

    // GPU
    @ExcelColumn("GPU - Card màn hình")
    private String gpuCard;

    @ExcelColumn("GPU - Số nhân")
    private String gpuCores;

    @ExcelColumn("GPU - TGP")
    private String gpuTgp;

    @ExcelColumn("GPU - TOPS")
    private String gpuAiPerformanceTops;

    // RAM
    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - RAM")
    private String ram;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Loại RAM")
    private String ramType;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Tốc độ Bus RAM")
    private String ramBusSpeed;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Hỗ trợ RAM tối đa")
    private String maxRam;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Ổ cứng")
    private String storage;

    // Screen
    @ExcelColumn("Màn hình - Kích thước màn hình")
    private String screenSize;

    @ExcelColumn("Màn hình - Độ phân giải")
    private String screenResolution;

    @ExcelColumn("Màn hình - Tấm nền")
    private String panel;

    @ExcelColumn("Màn hình - Tần số quét")
    private String refreshRate;

    @ExcelColumn("Màn hình - Độ phủ màu")
    private String colorGamut;

    @ExcelColumn("Màn hình - Màn hình cảm ứng")
    private String touchScreen;

    @ExcelColumn("Màn hình - Công nghệ màn hình")
    private String displayTechnology;

    // I/O
    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Cổng giao tiếp")
    private String ports;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Kết nối không dây")
    private String wireless;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Webcam")
    private String webcam;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Đèn bàn phím")
    private String keyboardBacklight;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Bảo mật")
    private String security;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Công nghệ âm thanh")
    private String audioTechnology;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Tản nhiệt")
    private String cooling;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Tính năng khác")
    private String otherFeatures;

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Khe đọc thẻ nhớ")
    private String memoryCardReader;

    // Battery & OS
    @ExcelColumn("Kích thước - Khối lượng - Pin - Thông tin Pin")
    private String battery;

    @ExcelColumn("Kích thước - Khối lượng - Pin - Hệ điều hành")
    private String operatingSystem;

    @ExcelColumn("Kích thước - Khối lượng - Pin - Thời điểm ra mắt")
    private String releaseTime;

    @ExcelColumn("Kích thước - Khối lượng - Pin - Kích thước")
    private String dimensionsWeight;

    @ExcelColumn("Kích thước - Khối lượng - Pin - Chất liệu")
    private String material;

    @ExcelColumn("Số lượng tồn kho")
    private Integer stockQuantity;

    @ExcelColumn("Ngưỡng cảnh báo tồn")
    private Integer lowStockThreshold;

    private String category;
    private String tag;
    private String shortDescription;
    private String specsJson;
    private String reviewsJson;
    private Double rating;
    private Integer reviewCount;
}
