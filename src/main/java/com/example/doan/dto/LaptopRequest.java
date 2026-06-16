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
    @ExcelColumn("GPU - Card màn hình")
    private String gpuCard;

    @ExcelColumn("GPU - Số nhân")
    private Integer gpuCores;

    @ExcelColumn("GPU - TGP")
    private Integer gpuTgp;

    @ExcelColumn("GPU - AI TOPS")
    private Double gpuAiPerformanceTops;

    // RAM
    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - RAM")
    private String ram;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Loại RAM")
    private String ramType;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Tốc độ Bus RAM")
    private Integer ramBusSpeed;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Hỗ trợ RAM tối đa")
    private String maxRam;

    @ExcelColumn("Bộ nhớ RAM, Ổ cứng - Ổ cứng")
    private String storage;

    // Screen
    @ExcelColumn("Màn hình - Kích thước màn hình")
    private String screenSize;

    @ExcelColumn("Màn hình - Độ phân giải")
    private String screenResolution;

    @ExcelColumn("Màn hình - Tấm nền Panel")
    private String panel;

    @ExcelColumn("Màn hình - Tần số quét")
    private Integer refreshRate;

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

    @ExcelColumn("Cổng kết nối & tính năng mở rộng - Khe đọc thẻ nhớ")
    private String memoryCardReader;

    private String category;
    private String tag;
    private String shortDescription;
    private String specsJson;
    private String reviewsJson;
    private Double rating;
    private Integer reviewCount;
}