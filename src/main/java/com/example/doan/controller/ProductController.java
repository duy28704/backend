package com.example.doan.controller;

import com.example.doan.dto.LaptopRequest;
import com.example.doan.entity.Laptop;
import com.example.doan.response.ApiResponse;
import com.example.doan.response.ExcelResult;
import com.example.doan.service.CloudinaryService;
import com.example.doan.service.ProductService;
import com.example.doan.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    // =========================
    // GET ALL PRODUCTS
    // =========================
    @GetMapping
    public ResponseEntity<ApiResponse<List<Laptop>>> getAllProducts(
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") boolean includeDeleted
    ) {
        List<Laptop> laptops = productService.findAllLaptops(includeDeleted);
        ApiResponse<List<Laptop>> response = ApiResponse.<List<Laptop>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get all products success")
                .data(laptops)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // GET PRODUCT BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Laptop>> getProductById(@PathVariable Long id) {
        Laptop laptop = productService.findLaptopById(id);
        ApiResponse<Laptop> response = ApiResponse.<Laptop>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get product by id success")
                .data(laptop)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // CREATE PRODUCT
    // =========================
    @PostMapping
    public ResponseEntity<ApiResponse<Laptop>> createProduct(@RequestBody LaptopRequest request) {
        Laptop laptop = productService.createLaptop(request);
        ApiResponse<Laptop> response = ApiResponse.<Laptop>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Create product success")
                .data(laptop)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================
    // UPDATE PRODUCT
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Laptop>> updateProduct(
            @PathVariable Long id,
            @RequestBody LaptopRequest request
    ) {
        Laptop laptop = productService.updateLaptop(id, request);
        ApiResponse<Laptop> response = ApiResponse.<Laptop>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Update product success")
                .data(laptop)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // DELETE PRODUCT
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(value = "hard", required = false, defaultValue = "false") boolean hard
    ) {
        boolean hasHardDeletePermission = user != null && user.getAuthorities().stream()
                .anyMatch(a -> "product.hard-delete".equals(a.getAuthority()));
        if (hard && !hasHardDeletePermission) {
            throw new RuntimeException("Bạn không có quyền xóa vĩnh viễn sản phẩm khỏi hệ thống.");
        }
        
        if (hard) {
            productService.hardDeleteLaptop(id);
        } else {
            productService.softDeleteLaptop(id);
        }
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message(hard ? "Hard delete product success" : "Soft delete product success")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // IMPORT PRODUCTS BY EXCEL
    // =========================
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ExcelResult<Laptop>>> importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        ExcelResult<Laptop> result = productService.importExcelAndSave(file);
        
        ApiResponse<ExcelResult<Laptop>> response = ApiResponse.<ExcelResult<Laptop>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message(result.isHasError() ? "Import products completed with errors" : "Import products success")
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // GET DELETED PRODUCTS (TRASH BIN)
    // =========================
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<List<Laptop>>> getDeletedProducts() {
        List<Laptop> laptops = productService.findAllLaptops(true);
        List<Laptop> deletedLaptops = laptops.stream().filter(p -> p.isDeleted()).toList();
        
        ApiResponse<List<Laptop>> response = ApiResponse.<List<Laptop>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get deleted products success")
                .data(deletedLaptops)
                .build();
        return ResponseEntity.ok(response);
    }

    // =========================
    // RESTORE PRODUCT
    // =========================
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Laptop>> restoreProduct(@PathVariable Long id) {
        Laptop laptop = productService.restoreLaptop(id);
        ApiResponse<Laptop> response = ApiResponse.<Laptop>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Restore product success")
                .data(laptop)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug-file/{id}")
    public ResponseEntity<String> debugToFile(@PathVariable Long id) {
        try {
            Laptop laptop = productService.findLaptopById(id);
            java.io.FileWriter writer = new java.io.FileWriter("d:/doan/backend/debug-laptop.txt");
            writer.write("ID: " + laptop.getId() + "\n");
            writer.write("Name: " + laptop.getName() + "\n");
            writer.write("SpecsJson: " + laptop.getSpecsJson() + "\n");
            writer.write("cpuTechnology: " + laptop.getCpuTechnology() + "\n");
            writer.write("cpuCores: " + laptop.getCpuCores() + "\n");
            writer.write("cpuThreads: " + laptop.getCpuThreads() + "\n");
            writer.write("cpuSpeed: " + laptop.getCpuSpeed() + "\n");
            writer.write("npu: " + laptop.getNpu() + "\n");
            writer.write("gpuCard: " + laptop.getGpuCard() + "\n");
            writer.write("ram: " + laptop.getRam() + "\n");
            writer.write("storage: " + laptop.getStorage() + "\n");
            writer.write("screenSize: " + laptop.getScreenSize() + "\n");
            writer.write("screenResolution: " + laptop.getScreenResolution() + "\n");
            writer.write("panel: " + laptop.getPanel() + "\n");
            writer.write("refreshRate: " + laptop.getRefreshRate() + "\n");
            writer.write("displayTechnology: " + laptop.getDisplayTechnology() + "\n");
            writer.write("ports: " + laptop.getPorts() + "\n");
            writer.write("wireless: " + laptop.getWireless() + "\n");
            writer.write("battery: " + laptop.getBattery() + "\n");
            writer.write("operatingSystem: " + laptop.getOperatingSystem() + "\n");
            writer.write("dimensionsWeight: " + laptop.getDimensionsWeight() + "\n");
            writer.close();
            return ResponseEntity.ok("Dumped to debug-laptop.txt");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // =========================
    // UPLOAD IMAGE TO CLOUDINARY
    // =========================
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadFile(file);
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .message("Upload image success")
                    .data(url)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Upload image failed: " + e.getMessage())
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
