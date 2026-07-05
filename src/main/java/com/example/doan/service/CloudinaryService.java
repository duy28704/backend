package com.example.doan.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String uploadFile(MultipartFile file) throws IOException {
        log.info("Bắt đầu tải tệp tin lên Cloudinary: tên='{}', kích thước={} bytes", file.getOriginalFilename(), file.getSize());
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = uploadResult.get("secure_url").toString();
            log.info("Tải tệp tin lên Cloudinary thành công, URL: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Tải tệp tin lên Cloudinary thất bại cho tệp '{}'. Chi tiết lỗi: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    public String extractPublicId(String url) {
        if (url == null || url.trim().isEmpty() || !url.contains("/image/upload/")) {
            return null;
        }
        try {
            int uploadIndex = url.indexOf("/image/upload/");
            String afterUpload = url.substring(uploadIndex + "/image/upload/".length());
            
            // Remove version segment if present, e.g. v1571218039/
            if (afterUpload.startsWith("v")) {
                int firstSlash = afterUpload.indexOf('/');
                if (firstSlash != -1) {
                    String possibleVersion = afterUpload.substring(1, firstSlash);
                    if (possibleVersion.matches("\\d+")) {
                        afterUpload = afterUpload.substring(firstSlash + 1);
                    }
                }
            }
            
            // Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot != -1) {
                afterUpload = afterUpload.substring(0, lastDot);
            }
            
            return afterUpload;
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất public_id từ URL Cloudinary: {}", url, e);
            return null;
        }
    }

    public void deleteFile(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        String publicId = extractPublicId(url);
        if (publicId != null) {
            log.info("Bắt đầu xóa tệp tin trên Cloudinary, public_id: {}", publicId);
            try {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Xóa tệp tin trên Cloudinary thành công cho public_id '{}', kết quả: {}", publicId, result);
            } catch (IOException e) {
                log.error("Xóa tệp tin trên Cloudinary thất bại cho public_id '{}'. Chi tiết lỗi: {}", publicId, e.getMessage(), e);
            }
        }
    }
}
