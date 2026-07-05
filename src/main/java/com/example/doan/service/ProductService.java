package com.example.doan.service;


import com.example.doan.dto.LaptopExcelRequest;
import com.example.doan.dto.LaptopRequest;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.Product;
import com.example.doan.excel.ExcelColumn;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.response.ExcelError;
import com.example.doan.response.ExcelResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class ProductService {

    private final LaptopRepository laptopRepository;
    private final ExcelProductService excelProductService;
    private final CloudinaryService cloudinaryService;
    private final SearchService searchService;

    public ProductService(
            LaptopRepository laptopRepository,
            ExcelProductService excelProductService,
            CloudinaryService cloudinaryService,
            SearchService searchService
    ) {
        this.laptopRepository = laptopRepository;
        this.excelProductService = excelProductService;
        this.cloudinaryService = cloudinaryService;
        this.searchService = searchService;
    }

    public List<Laptop> findAllLaptops(boolean includeDeleted) {
        log.info("Tìm kiếm tất cả sản phẩm laptop (bao gồm cả sản phẩm đã xóa: {})", includeDeleted);
        if (includeDeleted) {
            return laptopRepository.findAll();
        }

        return laptopRepository.findByDeleted(false);
    }

    public Laptop findLaptopById(Long id) {
        log.info("Tìm kiếm sản phẩm laptop theo ID={}", id);
        return laptopRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm laptop với ID={}", id);
                    return new EntityNotFoundException("Không tìm thấy laptop id=" + id);
                });
    }

    @Transactional
    public Laptop createLaptop(LaptopRequest request) {
        log.info("Bắt đầu tạo sản phẩm laptop mới: tên='{}'", request.getName());
        Laptop laptop = new Laptop();
        applyRequest(laptop, request);
        Laptop saved = laptopRepository.save(laptop);
        log.info("Tạo sản phẩm laptop thành công: ID={}, Tên='{}'", saved.getId(), saved.getName());
        searchService.indexProduct(saved);
        return saved;
    }

    @Transactional
    public List<Laptop> createManyLaptops(List<LaptopRequest> requests) {
        log.info("Bắt đầu tạo nhiều sản phẩm laptop cùng lúc, số lượng yêu cầu: {}", requests.size());
        List<Laptop> laptops = new ArrayList<>();

        for (LaptopRequest request : requests) {
            Laptop laptop = new Laptop();
            applyRequest(laptop, request);
            laptops.add(laptop);
        }

        List<Laptop> savedList = laptopRepository.saveAll(laptops);
        log.info("Đã lưu thành công danh sách gồm {} sản phẩm laptop mới", savedList.size());
        for (Laptop savedLaptop : savedList) {
            searchService.indexProduct(savedLaptop);
        }
        return savedList;
    }

    @Transactional
    public Laptop updateLaptop(Long id, LaptopRequest request) {
        log.info("Bắt đầu cập nhật thông tin sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        applyRequest(laptop, request);
        Laptop saved = laptopRepository.save(laptop);
        log.info("Cập nhật thông tin sản phẩm laptop ID={} thành công", id);
        searchService.indexProduct(saved);
        return saved;
    }

    @Transactional
    public void softDeleteLaptop(Long id) {
        log.info("Thực hiện xóa tạm thời sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        laptop.setDeleted(true);
        laptop.setDeletedAt(Instant.now());
        laptop.setUpdatedAt(Instant.now());
        laptopRepository.save(laptop);
        log.info("Đã xóa tạm thời sản phẩm laptop ID={} thành công", id);
        searchService.deleteProduct(id);
    }

    @Transactional
    public void hardDeleteLaptop(Long id) {
        log.info("Thực hiện xóa vĩnh viễn sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        
        // Delete images from Cloudinary
        deleteProductImagesFromCloudinary(laptop);
        
        searchService.deleteProduct(id);
        
        laptopRepository.delete(laptop);
        log.info("Đã xóa vĩnh viễn sản phẩm laptop ID={} thành công", id);
    }

    private void deleteProductImagesFromCloudinary(Laptop laptop) {
        String imagesStr = laptop.getImages();
        if (imagesStr != null && !imagesStr.trim().isEmpty()) {
            String[] urls = imagesStr.split("[,\\s]+");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.isEmpty() && !trimmedUrl.contains("/assets/")) {
                    log.info("Yêu cầu CloudinaryService xóa ảnh: {}", trimmedUrl);
                    try {
                        cloudinaryService.deleteFile(trimmedUrl);
                    } catch (Exception e) {
                        log.error("Lỗi khi gọi CloudinaryService để xóa ảnh: {}", trimmedUrl, e);
                    }
                }
            }
        }
    }

    @Transactional
    public Laptop restoreLaptop(Long id) {
        log.info("Khôi phục lại sản phẩm laptop đã bị xóa ID={}", id);
        Laptop laptop = findLaptopById(id);
        laptop.setDeleted(false);
        laptop.setDeletedAt(null);
        laptop.setUpdatedAt(Instant.now());
        Laptop saved = laptopRepository.save(laptop);
        log.info("Khôi phục sản phẩm laptop ID={} thành công", id);
        searchService.indexProduct(saved);
        return saved;
    }

    @Transactional
    public ExcelResult<Laptop> importExcelAndSave(MultipartFile file) throws Exception {
        log.info("Bắt đầu xử lý import dữ liệu sản phẩm từ file Excel: {}", file.getOriginalFilename());
        ExcelResult<LaptopExcelRequest> parsed = excelProductService.importExcel(file, LaptopExcelRequest.class);
        List<Laptop> savedLaptops = new ArrayList<>();
        if (!parsed.isHasError() && parsed.getData() != null && !parsed.getData().isEmpty()) {
            savedLaptops = createManyLaptopsFromExcel(parsed.getData());
            log.info("Import file Excel thành công, đã lưu thêm {} sản phẩm mới", savedLaptops.size());
        } else if (parsed.isHasError()) {
            log.warn("Import file Excel hoàn tất nhưng có lỗi, số dòng bị lỗi: {}", parsed.getErrors().size());
        }
        return new ExcelResult<>(savedLaptops, parsed.getErrors());
    }

    @Transactional
    public List<Laptop> createManyLaptopsFromExcel(List<LaptopExcelRequest> requests) {
        log.info("Đang tạo danh sách gồm {} sản phẩm laptop từ dữ liệu Excel", requests.size());
        List<Laptop> laptops = new ArrayList<>();

        for (LaptopExcelRequest request : requests) {
            Laptop laptop = new Laptop();
            applyExcelRequest(laptop, request);
            laptops.add(laptop);
        }

        List<Laptop> savedList = laptopRepository.saveAll(laptops);
        log.info("Đã lưu danh sách import gồm {} sản phẩm vào cơ sở dữ liệu thành công", savedList.size());
        for (Laptop savedLaptop : savedList) {
            searchService.indexProduct(savedLaptop);
        }
        return savedList;
    }

    private void applyRequest(Laptop laptop, LaptopRequest request) {
        laptop.setName(request.getName().trim());
        laptop.setPrice(trimToNull(request.getPrice()));
        laptop.setLink(trimToNull(request.getLink()));
        laptop.setImages(trimToNull(request.getImages()));
        laptop.setBrand(trimToNull(request.getBrand()));
        laptop.setDescription(trimToNull(request.getDescription()));

        laptop.setCpuTechnology(trimToNull(request.getCpuTechnology()));
        laptop.setCpuCores(trimToNull(request.getCpuCores()));
        laptop.setCpuThreads(trimToNull(request.getCpuThreads()));
        laptop.setCpuSpeed(trimToNull(request.getCpuSpeed()));
        laptop.setNpu(trimToNull(request.getNpu()));
        laptop.setCpuAiPerformanceTops(trimToNull(request.getCpuAiPerformanceTops()));

        laptop.setGpuCard(trimToNull(request.getGpuCard()));
        laptop.setGpuCores(trimToNull(request.getGpuCores()));
        laptop.setGpuTgp(trimToNull(request.getGpuTgp()));
        laptop.setGpuAiPerformanceTops(trimToNull(request.getGpuAiPerformanceTops()));

        laptop.setRam(trimToNull(request.getRam()));
        laptop.setRamType(trimToNull(request.getRamType()));
        laptop.setRamBusSpeed(trimToNull(request.getRamBusSpeed()));
        laptop.setMaxRam(trimToNull(request.getMaxRam()));
        laptop.setStorage(trimToNull(request.getStorage()));

        laptop.setScreenSize(trimToNull(request.getScreenSize()));
        laptop.setScreenResolution(trimToNull(request.getScreenResolution()));
        laptop.setPanel(trimToNull(request.getPanel()));
        laptop.setRefreshRate(trimToNull(request.getRefreshRate()));
        laptop.setColorGamut(trimToNull(request.getColorGamut()));
        laptop.setTouchScreen(trimToNull(request.getTouchScreen()));
        laptop.setDisplayTechnology(trimToNull(request.getDisplayTechnology()));

        laptop.setPorts(trimToNull(request.getPorts()));
        laptop.setWireless(trimToNull(request.getWireless()));
        laptop.setWebcam(trimToNull(request.getWebcam()));
        laptop.setKeyboardBacklight(trimToNull(request.getKeyboardBacklight()));
        laptop.setSecurity(trimToNull(request.getSecurity()));
        laptop.setAudioTechnology(trimToNull(request.getAudioTechnology()));
        laptop.setCooling(trimToNull(request.getCooling()));
        laptop.setOtherFeatures(trimToNull(request.getOtherFeatures()));

        laptop.setBattery(trimToNull(request.getBattery()));
        laptop.setOperatingSystem(trimToNull(request.getOperatingSystem()));
        laptop.setReleaseTime(trimToNull(request.getReleaseTime()));
        laptop.setDimensionsWeight(trimToNull(request.getDimensionsWeight()));
        laptop.setMaterial(trimToNull(request.getMaterial()));
        laptop.setMemoryCardReader(trimToNull(request.getMemoryCardReader()));

        laptop.setCategory(trimToNull(request.getCategory()));
        laptop.setTag(trimToNull(request.getTag()));
        laptop.setShortDescription(trimToNull(request.getShortDescription()));
        laptop.setSpecsJson(trimToNull(request.getSpecsJson()));
        laptop.setReviewsJson(trimToNull(request.getReviewsJson()));
        laptop.setRating(request.getRating() != null ? request.getRating() : 5.0);
        laptop.setReviewCount(request.getReviewCount() != null ? request.getReviewCount() : 0);

        laptop.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 50);
        laptop.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10);

        laptop.setDeleted(false);
        laptop.setDeletedAt(null);
        laptop.setUpdatedAt(Instant.now());
    }

    private void applyExcelRequest(Laptop laptop, LaptopExcelRequest request) {
        laptop.setName(request.getName().trim());
        laptop.setPrice(trimToNull(request.getPrice()));
        laptop.setLink(trimToNull(request.getLink()));
        laptop.setImages(trimToNull(request.getImages()));
        laptop.setBrand(trimToNull(request.getBrand()));
        laptop.setDescription(trimToNull(request.getDescription()));

        laptop.setCpuTechnology(trimToNull(request.getCpuTechnology()));
        laptop.setCpuCores(trimToNull(request.getCpuCores()));
        laptop.setCpuThreads(trimToNull(request.getCpuThreads()));
        laptop.setCpuSpeed(trimToNull(request.getCpuSpeed()));
        laptop.setNpu(trimToNull(request.getNpu()));
        laptop.setCpuAiPerformanceTops(trimToNull(request.getCpuAiPerformanceTops()));

        laptop.setGpuCard(trimToNull(request.getGpuCard()));
        laptop.setGpuCores(trimToNull(request.getGpuCores()));
        laptop.setGpuTgp(trimToNull(request.getGpuTgp()));
        laptop.setGpuAiPerformanceTops(trimToNull(request.getGpuAiPerformanceTops()));

        laptop.setRam(trimToNull(request.getRam()));
        laptop.setRamType(trimToNull(request.getRamType()));
        laptop.setRamBusSpeed(trimToNull(request.getRamBusSpeed()));
        laptop.setMaxRam(trimToNull(request.getMaxRam()));
        laptop.setStorage(trimToNull(request.getStorage()));

        laptop.setScreenSize(trimToNull(request.getScreenSize()));
        laptop.setScreenResolution(trimToNull(request.getScreenResolution()));
        laptop.setPanel(trimToNull(request.getPanel()));
        laptop.setRefreshRate(trimToNull(request.getRefreshRate()));
        laptop.setColorGamut(trimToNull(request.getColorGamut()));
        laptop.setTouchScreen(trimToNull(request.getTouchScreen()));
        laptop.setDisplayTechnology(trimToNull(request.getDisplayTechnology()));

        laptop.setPorts(trimToNull(request.getPorts()));
        laptop.setWireless(trimToNull(request.getWireless()));
        laptop.setWebcam(trimToNull(request.getWebcam()));
        laptop.setKeyboardBacklight(trimToNull(request.getKeyboardBacklight()));
        laptop.setSecurity(trimToNull(request.getSecurity()));
        laptop.setAudioTechnology(trimToNull(request.getAudioTechnology()));
        laptop.setCooling(trimToNull(request.getCooling()));
        laptop.setOtherFeatures(trimToNull(request.getOtherFeatures()));

        laptop.setBattery(trimToNull(request.getBattery()));
        laptop.setOperatingSystem(trimToNull(request.getOperatingSystem()));
        laptop.setReleaseTime(trimToNull(request.getReleaseTime()));
        laptop.setDimensionsWeight(trimToNull(request.getDimensionsWeight()));
        laptop.setMaterial(trimToNull(request.getMaterial()));
        laptop.setMemoryCardReader(trimToNull(request.getMemoryCardReader()));

        laptop.setCategory(trimToNull(request.getCategory()));
        laptop.setTag(trimToNull(request.getTag()));
        laptop.setShortDescription(trimToNull(request.getShortDescription()));
        laptop.setSpecsJson(trimToNull(request.getSpecsJson()));
        laptop.setReviewsJson(trimToNull(request.getReviewsJson()));
        laptop.setRating(request.getRating() != null ? request.getRating() : 5.0);
        laptop.setReviewCount(request.getReviewCount() != null ? request.getReviewCount() : 0);

        laptop.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 50);
        laptop.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10);

        laptop.setDeleted(false);
        laptop.setDeletedAt(null);
        laptop.setUpdatedAt(Instant.now());
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        return trimToNull(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

