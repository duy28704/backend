package com.example.doan.controller;

import com.example.doan.dto.SearchStatDto;
import com.example.doan.entity.Laptop;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Laptop>>> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "autocomplete", required = false, defaultValue = "false") Boolean autocomplete,
            @RequestParam(value = "fuzzy", required = false, defaultValue = "true") Boolean fuzzy,
            HttpServletRequest request
    ) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        List<Laptop> results = searchService.search(query, minPrice, maxPrice, brand, category, autocomplete, fuzzy, clientIp);

        ApiResponse<List<Laptop>> response = ApiResponse.<List<Laptop>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Search success")
                .data(results)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SearchStatDto>> getStats() {
        SearchStatDto stats = searchService.getSearchStatistics();
        ApiResponse<SearchStatDto> response = ApiResponse.<SearchStatDto>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get search stats success")
                .data(stats)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> syncAll() {
        searchService.syncAll();
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Sync search index success")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
