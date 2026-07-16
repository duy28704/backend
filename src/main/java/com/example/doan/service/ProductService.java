package com.example.doan.service;


import com.example.doan.dto.LaptopExcelRequest;
import com.example.doan.dto.LaptopRequest;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.NotificationType;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final NotificationService notificationService;

    public ProductService(
            LaptopRepository laptopRepository,
            ExcelProductService excelProductService,
            CloudinaryService cloudinaryService,
            SearchService searchService,
            NotificationService notificationService
    ) {
        this.laptopRepository = laptopRepository;
        this.excelProductService = excelProductService;
        this.cloudinaryService = cloudinaryService;
        this.searchService = searchService;
        this.notificationService = notificationService;
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin người dùng hiện tại: {}", e.getMessage());
        }
        return "Hệ thống";
    }

    @Cacheable(value = "laptops", key = "#includeDeleted")
    public List<Laptop> findAllLaptops(boolean includeDeleted) {
        log.info("Tìm kiếm tất cả sản phẩm laptop (bao gồm cả sản phẩm đã xóa: {})", includeDeleted);
        if (includeDeleted) {
            return laptopRepository.findAll();
        }

        return laptopRepository.findByDeleted(false);
    }

    @Cacheable(value = "laptop", key = "#id")
    public Laptop findLaptopById(Long id) {
        log.info("Tìm kiếm sản phẩm laptop theo ID={}", id);
        return laptopRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm laptop với ID={}", id);
                    return new EntityNotFoundException("Không tìm thấy laptop id=" + id);
                });
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public Laptop createLaptop(LaptopRequest request) {
        log.info("Bắt đầu tạo sản phẩm laptop mới: tên='{}'", request.getName());
        Laptop laptop = new Laptop();
        applyRequest(laptop, request);
        Laptop saved = laptopRepository.save(laptop);
        log.info("Tạo sản phẩm laptop thành công: ID={}, Tên='{}'", saved.getId(), saved.getName());
        searchService.indexProduct(saved);

        // Gửi thông báo cho Admin/Staff
        try {
            String operator = getCurrentUsername();
            notificationService.notifyAllAdmins(
                    "Sản phẩm mới: " + saved.getName(),
                    operator + " đã thêm sản phẩm '" + saved.getName() + "' (ID " + saved.getId() + ") vào hệ thống.",
                    NotificationType.SYSTEM,
                    String.valueOf(saved.getId()),
                    "/admin/products"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo thêm sản phẩm: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
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
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public Laptop updateLaptop(Long id, LaptopRequest request) {
        log.info("Bắt đầu cập nhật thông tin sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        applyRequest(laptop, request);
        Laptop saved = laptopRepository.save(laptop);
        log.info("Cập nhật thông tin sản phẩm laptop ID={} thành công", id);
        searchService.indexProduct(saved);

        // Gửi thông báo cho Admin/Staff
        try {
            String operator = getCurrentUsername();
            notificationService.notifyAllAdmins(
                    "Cập nhật sản phẩm: " + saved.getName(),
                    operator + " đã cập nhật sản phẩm '" + saved.getName() + "' (ID " + saved.getId() + ").",
                    NotificationType.SYSTEM,
                    String.valueOf(saved.getId()),
                    "/admin/products"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo cập nhật sản phẩm: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public void softDeleteLaptop(Long id) {
        log.info("Thực hiện xóa tạm thời sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        String productName = laptop.getName();
        laptop.setDeleted(true);
        laptop.setDeletedAt(Instant.now());
        laptop.setUpdatedAt(Instant.now());
        laptopRepository.save(laptop);
        log.info("Đã xóa tạm thời sản phẩm laptop ID={} thành công", id);
        searchService.deleteProduct(id);

        // Gửi thông báo cho Admin/Staff
        try {
            String operator = getCurrentUsername();
            notificationService.notifyAllAdmins(
                    "Xóa sản phẩm: " + productName,
                    operator + " đã chuyển sản phẩm '" + productName + "' (ID " + id + ") vào thùng rác.",
                    NotificationType.SYSTEM,
                    String.valueOf(id),
                    "/admin/trash"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo xóa sản phẩm: {}", e.getMessage());
        }
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public void softDeleteLaptops(List<Long> ids) {
        log.info("Thực hiện xóa tạm thời danh sách sản phẩm laptop IDs={}", ids);
        List<String> deletedNames = new ArrayList<>();
        for (Long id : ids) {
            try {
                Laptop laptop = findLaptopById(id);
                deletedNames.add(laptop.getName());
                laptop.setDeleted(true);
                laptop.setDeletedAt(Instant.now());
                laptop.setUpdatedAt(Instant.now());
                laptopRepository.save(laptop);
                searchService.deleteProduct(id);
            } catch (Exception e) {
                log.error("Lỗi khi xóa tạm thời sản phẩm ID={}: {}", id, e.getMessage());
            }
        }
        log.info("Đã hoàn tất xóa tạm thời danh sách sản phẩm");

        // Gửi thông báo cho Admin/Staff
        if (!deletedNames.isEmpty()) {
            try {
                String operator = getCurrentUsername();
                notificationService.notifyAllAdmins(
                        "Xóa hàng loạt: " + deletedNames.size() + " sản phẩm",
                        operator + " đã chuyển " + deletedNames.size() + " sản phẩm vào thùng rác: " + String.join(", ", deletedNames) + ".",
                        NotificationType.SYSTEM,
                        null,
                        "/admin/trash"
                );
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo xóa hàng loạt: {}", e.getMessage());
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public void hardDeleteLaptop(Long id) {
        log.info("Thực hiện xóa vĩnh viễn sản phẩm laptop ID={}", id);
        Laptop laptop = findLaptopById(id);
        String productName = laptop.getName();
        
        // Delete images from Cloudinary
        deleteProductImagesFromCloudinary(laptop);
        
        searchService.deleteProduct(id);
        
        laptopRepository.delete(laptop);
        log.info("Đã xóa vĩnh viễn sản phẩm laptop ID={} thành công", id);

        // Gửi thông báo cho Admin/Staff
        try {
            String operator = getCurrentUsername();
            notificationService.notifyAllAdmins(
                    "Xóa vĩnh viễn: " + productName,
                    operator + " đã xóa vĩnh viễn sản phẩm '" + productName + "' (ID " + id + ") khỏi hệ thống.",
                    NotificationType.SYSTEM,
                    null,
                    "/admin/products"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo xóa vĩnh viễn sản phẩm: {}", e.getMessage());
        }
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public void hardDeleteLaptops(List<Long> ids) {
        log.info("Thực hiện xóa vĩnh viễn danh sách sản phẩm laptop IDs={}", ids);
        List<String> deletedNames = new ArrayList<>();
        for (Long id : ids) {
            try {
                Laptop laptop = findLaptopById(id);
                deletedNames.add(laptop.getName());
                deleteProductImagesFromCloudinary(laptop);
                searchService.deleteProduct(id);
                laptopRepository.delete(laptop);
            } catch (Exception e) {
                log.error("Lỗi khi xóa vĩnh viễn sản phẩm ID={}: {}", id, e.getMessage());
            }
        }
        log.info("Đã hoàn tất xóa vĩnh viễn danh sách sản phẩm");

        // Gửi thông báo cho Admin/Staff
        if (!deletedNames.isEmpty()) {
            try {
                String operator = getCurrentUsername();
                notificationService.notifyAllAdmins(
                        "Xóa vĩnh viễn: " + deletedNames.size() + " sản phẩm",
                        operator + " đã xóa vĩnh viễn " + deletedNames.size() + " sản phẩm: " + String.join(", ", deletedNames) + ".",
                        NotificationType.SYSTEM,
                        null,
                        "/admin/products"
                );
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo xóa vĩnh viễn hàng loạt: {}", e.getMessage());
            }
        }
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
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public Laptop restoreLaptop(Long id) {
        log.info("Khôi phục lại sản phẩm laptop đã bị xóa ID={}", id);
        Laptop laptop = findLaptopById(id);
        laptop.setDeleted(false);
        laptop.setDeletedAt(null);
        laptop.setUpdatedAt(Instant.now());
        Laptop saved = laptopRepository.save(laptop);
        log.info("Khôi phục sản phẩm laptop ID={} thành công", id);
        searchService.indexProduct(saved);

        // Gửi thông báo cho Admin/Staff
        try {
            String operator = getCurrentUsername();
            notificationService.notifyAllAdmins(
                    "Khôi phục sản phẩm: " + saved.getName(),
                    operator + " đã khôi phục sản phẩm '" + saved.getName() + "' (ID " + saved.getId() + ") từ thùng rác.",
                    NotificationType.SYSTEM,
                    String.valueOf(saved.getId()),
                    "/admin/products"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo khôi phục sản phẩm: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public void restoreLaptops(List<Long> ids) {
        log.info("Khôi phục danh sách sản phẩm laptop đã bị xóa IDs={}", ids);
        for (Long id : ids) {
            try {
                Laptop laptop = findLaptopById(id);
                laptop.setDeleted(false);
                laptop.setDeletedAt(null);
                laptop.setUpdatedAt(Instant.now());
                Laptop saved = laptopRepository.save(laptop);
                searchService.indexProduct(saved);
            } catch (Exception e) {
                log.error("Lỗi khi khôi phục sản phẩm ID={}: {}", id, e.getMessage());
            }
        }
        log.info("Đã hoàn tất khôi phục danh sách sản phẩm");
    }

    @Transactional
    @CacheEvict(value = {"laptops", "laptop"}, allEntries = true)
    public ExcelResult<Laptop> importExcelAndSave(MultipartFile file) throws Exception {
        log.info("Bắt đầu xử lý import dữ liệu sản phẩm từ file Excel: {}", file.getOriginalFilename());
        ExcelResult<LaptopExcelRequest> parsed = excelProductService.importExcel(file, LaptopExcelRequest.class);
        List<Laptop> savedLaptops = new ArrayList<>();
        if (!parsed.isHasError() && parsed.getData() != null && !parsed.getData().isEmpty()) {
            savedLaptops = createManyLaptopsFromExcel(parsed.getData());
            log.info("Import file Excel thành công, đã lưu thêm {} sản phẩm mới", savedLaptops.size());

            // Gửi thông báo cho Admin/Staff
            try {
                String operator = getCurrentUsername();
                notificationService.notifyAllAdmins(
                        "Import Excel: " + savedLaptops.size() + " sản phẩm",
                        operator + " đã import thành công " + savedLaptops.size() + " sản phẩm từ file '" + file.getOriginalFilename() + "'.",
                        NotificationType.SYSTEM,
                        null,
                        "/admin/products"
                );
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo import Excel: {}", e.getMessage());
            }
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
        laptop.setPrice(parseDouble(request.getPrice()));
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

        laptop.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        laptop.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 0);

        laptop.setDeleted(false);
        laptop.setDeletedAt(null);
        laptop.setUpdatedAt(Instant.now());
    }

    private void applyExcelRequest(Laptop laptop, LaptopExcelRequest request) {
        laptop.setName(request.getName().trim());
        laptop.setPrice(parseDouble(request.getPrice()));
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

        laptop.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        laptop.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 0);

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

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            String clean = str.replaceAll("[\\s₫$eE]", ""); // remove currency/spaces/exponent
            if (clean.matches(".*\\d+\\.\\d{3}.*")) {
                clean = clean.replace(".", "");
            } else if (clean.matches(".*\\d+,\\d{3}.*")) {
                clean = clean.replace(",", "");
            }
            clean = clean.replace(",", ".");
            return Double.parseDouble(clean);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi giá '{}' sang Double", value, e);
            return null;
        }
    }
}

