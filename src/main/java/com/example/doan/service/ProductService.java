package com.example.doan.service;


import com.example.doan.dto.LaptopRequest;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.Product;
import com.example.doan.excel.ExcelColumn;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.response.ExcelError;
import com.example.doan.response.ExcelResult;
import jakarta.persistence.EntityNotFoundException;
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
public class ProductService {

    private final LaptopRepository laptopRepository;
    private final ExcelProductService excelProductService;

    public ProductService(
            LaptopRepository laptopRepository,
            ExcelProductService excelProductService
    ) {
        this.laptopRepository = laptopRepository;
        this.excelProductService = excelProductService;
    }

    public List<Laptop> findAllLaptops(boolean includeDeleted) {
        if (includeDeleted) {
            return laptopRepository.findAll();
        }

        return laptopRepository.findByDeleted(false);
    }

    public Laptop findLaptopById(Long id) {
        return laptopRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy laptop id=" + id));
    }

    @Transactional
    public Laptop createLaptop(LaptopRequest request) {
        Laptop laptop = new Laptop();
        applyRequest(laptop, request);
        return laptopRepository.save(laptop);
    }

    @Transactional
    public List<Laptop> createManyLaptops(List<LaptopRequest> requests) {
        List<Laptop> laptops = new ArrayList<>();

        for (LaptopRequest request : requests) {
            Laptop laptop = new Laptop();
            applyRequest(laptop, request);
            laptops.add(laptop);
        }

        return laptopRepository.saveAll(laptops);
    }

    @Transactional
    public Laptop updateLaptop(Long id, LaptopRequest request) {
        Laptop laptop = findLaptopById(id);
        applyRequest(laptop, request);
        return laptopRepository.save(laptop);
    }

    @Transactional
    public void softDeleteLaptop(Long id) {
        Laptop laptop = findLaptopById(id);
        laptop.deleted = true;
        laptop.deletedAt = Instant.now();
        laptop.updatedAt = Instant.now();
        laptopRepository.save(laptop);
    }

    @Transactional
    public void hardDeleteLaptop(Long id) {
        Laptop laptop = findLaptopById(id);
        laptopRepository.delete(laptop);
    }

    @Transactional
    public Laptop restoreLaptop(Long id) {
        Laptop laptop = findLaptopById(id);
        laptop.deleted = false;
        laptop.deletedAt = null;
        laptop.updatedAt = Instant.now();
        return laptopRepository.save(laptop);
    }

    @Transactional
    public ExcelResult<Laptop> importExcelAndSave(MultipartFile file) throws Exception {
        ExcelResult<LaptopRequest> parsed = excelProductService.importExcel(file, LaptopRequest.class);
        List<Laptop> savedLaptops = new ArrayList<>();
        if (!parsed.isHasError() && parsed.getData() != null && !parsed.getData().isEmpty()) {
            savedLaptops = createManyLaptops(parsed.getData());
        }
        return new ExcelResult<>(savedLaptops, parsed.getErrors());
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

        laptop.deleted = false;
        laptop.deletedAt = null;
        laptop.updatedAt = Instant.now();
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
