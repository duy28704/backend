package com.example.doan.service.impl;

import com.example.doan.document.LaptopDocument;
import com.example.doan.dto.SearchStatDto;
import com.example.doan.entity.Laptop;
import com.example.doan.entity.SearchLog;
import com.example.doan.repository.LaptopRepository;
import com.example.doan.repository.SearchLogRepository;
import com.example.doan.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final LaptopRepository laptopRepository;
    private final SearchLogRepository searchLogRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${elasticsearch.enabled:true}")
    private boolean esEnabled;

    @Override
    @Transactional
    public void indexProduct(Laptop laptop) {
        if (!esEnabled) return;
        try {
            LaptopDocument doc = mapToDocument(laptop);
            elasticsearchOperations.save(doc);
            log.info("Đã đánh chỉ mục sản phẩm ID={} lên Elasticsearch", laptop.getId());
        } catch (Exception e) {
            log.warn("Không thể lưu sản phẩm ID={} vào Elasticsearch (chế độ dự phòng đang hoạt động). Chi tiết: {}", laptop.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!esEnabled) return;
        try {
            elasticsearchOperations.delete(String.valueOf(id), LaptopDocument.class);
            log.info("Đã xóa chỉ mục sản phẩm ID={} khỏi Elasticsearch", id);
        } catch (Exception e) {
            log.warn("Không thể xóa sản phẩm ID={} khỏi Elasticsearch. Chi tiết: {}", id, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void syncAll() {
        log.info("Bắt đầu đồng bộ tất cả sản phẩm sang Elasticsearch");
        List<Laptop> laptops = laptopRepository.findByDeleted(false);
        int successCount = 0;
        for (Laptop laptop : laptops) {
            try {
                LaptopDocument doc = mapToDocument(laptop);
                elasticsearchOperations.save(doc);
                successCount++;
            } catch (Exception e) {
                log.warn("Lỗi khi đồng bộ sản phẩm ID={}: {}", laptop.getId(), e.getMessage());
            }
        }
        log.info("Hoàn thành đồng bộ sản phẩm. Thành công: {}/{}", successCount, laptops.size());
    }

    @Override
    @Transactional
    public List<Laptop> search(String queryText, Double minPrice, Double maxPrice, String brand, String category, Boolean autocomplete, Boolean fuzzy, String clientIp) {
        List<Laptop> results = new ArrayList<>();
        boolean usedEs = false;

        String cleanQuery = (queryText != null) ? queryText.trim() : "";

        if (esEnabled && !cleanQuery.isEmpty()) {
            try {
                results = searchElasticsearch(cleanQuery, minPrice, maxPrice, brand, category, autocomplete, fuzzy);
                usedEs = true;
                log.info("Tìm kiếm thành công bằng Elasticsearch cho từ khóa: {}", cleanQuery);
            } catch (Exception e) {
                log.warn("Elasticsearch gặp sự cố (Đang chuyển sang chế độ dự phòng SQL). Lỗi: {}", e.getMessage());
            }
        }

        // Fallback to DB
        if (!usedEs) {
            results = searchDb(cleanQuery, minPrice, maxPrice, brand, category, autocomplete, fuzzy);
        }

        // Log search query for analytics (skip logging empty or autocomplete queries to keep stats clean)
        if (autocomplete == null || !autocomplete) {
            try {
                SearchLog logEntry = SearchLog.builder()
                        .queryText(cleanQuery.isEmpty() ? "[Tất cả sản phẩm]" : cleanQuery)
                        .timestamp(Instant.now())
                        .resultCount(results.size())
                        .clientIp(clientIp)
                        .build();
                searchLogRepository.save(logEntry);
            } catch (Exception e) {
                log.error("Lỗi khi lưu lịch sử tìm kiếm: {}", e.getMessage());
            }
        }

        return results;
    }

    @Override
    public SearchStatDto getSearchStatistics() {
        long total = searchLogRepository.count();
        List<Map<String, Object>> top = searchLogRepository.findTopKeywords(PageRequest.of(0, 10));
        List<Map<String, Object>> zeros = searchLogRepository.findZeroResultKeywords(PageRequest.of(0, 10));
        List<Map<String, Object>> volume = searchLogRepository.findSearchVolumeOverTime();

        return SearchStatDto.builder()
                .totalSearches(total)
                .topKeywords(top)
                .zeroResultKeywords(zeros)
                .searchVolumeOverTime(volume)
                .build();
    }

    private List<Laptop> searchElasticsearch(String queryText, Double minPrice, Double maxPrice, String brand, String category, Boolean autocomplete, Boolean fuzzy) {
        Criteria criteria = new Criteria("deleted").is(false);

        if (autocomplete != null && autocomplete) {
            criteria = criteria.and(new Criteria("name").startsWith(queryText));
        } else if (fuzzy != null && fuzzy) {
            Criteria searchCriteria = new Criteria("name").fuzzy(queryText)
                    .or(new Criteria("brand").fuzzy(queryText))
                    .or(new Criteria("category").fuzzy(queryText))
                    .or(new Criteria("description").fuzzy(queryText));
            criteria = criteria.and(searchCriteria);
        } else {
            Criteria searchCriteria = new Criteria("name").contains(queryText)
                    .or(new Criteria("brand").contains(queryText))
                    .or(new Criteria("category").contains(queryText))
                    .or(new Criteria("description").contains(queryText));
            criteria = criteria.and(searchCriteria);
        }

        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }
        if (brand != null && !brand.trim().isEmpty() && !"all".equalsIgnoreCase(brand)) {
            criteria = criteria.and(new Criteria("brand").is(brand.toLowerCase()));
        }
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            criteria = criteria.and(new Criteria("category").is(category.toLowerCase()));
        }

        Query searchQuery = new CriteriaQuery(criteria);
        SearchHits<LaptopDocument> hits = elasticsearchOperations.search(searchQuery, LaptopDocument.class);
        List<Long> ids = hits.stream().map(SearchHit::getId).map(Long::parseLong).collect(Collectors.toList());

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Hydrate from DB to maintain sorted relevance returned by ES
        List<Laptop> dbLaptops = laptopRepository.findByIdIn(ids);
        Map<Long, Laptop> dbMap = dbLaptops.stream().collect(Collectors.toMap(Laptop::getId, l -> l));

        List<Laptop> sortedResult = new ArrayList<>();
        for (Long id : ids) {
            if (dbMap.containsKey(id)) {
                sortedResult.add(dbMap.get(id));
            }
        }
        return sortedResult;
    }

    private List<Laptop> searchDb(String queryText, Double minPrice, Double maxPrice, String brand, String category, Boolean autocomplete, Boolean fuzzy) {
        log.info("Đang thực hiện tìm kiếm dự phòng qua cơ sở dữ liệu cho từ khóa: '{}'", queryText);
        
        String cleanQuery = "%" + queryText.trim() + "%";
        String brandFilter = (brand != null && !brand.trim().isEmpty() && !"all".equalsIgnoreCase(brand)) ? brand : null;
        String categoryFilter = (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) ? category : null;

        List<Laptop> dbLaptops = laptopRepository.searchLaptopsWithFilters(queryText, brandFilter, categoryFilter);

        // Filter by price in memory since price is stored as String in DB
        if (minPrice != null || maxPrice != null) {
            dbLaptops = dbLaptops.stream().filter(l -> {
                try {
                    double p = Double.parseDouble(l.getPrice());
                    if (minPrice != null && p < minPrice) return false;
                    if (maxPrice != null && p > maxPrice) return false;
                } catch (Exception e) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        }

        // If fuzzy search is requested and normal query returns nothing, try Levenshtein similarity
        if ((fuzzy != null && fuzzy) && dbLaptops.isEmpty() && !queryText.trim().isEmpty()) {
            log.info("Tìm kiếm không kết quả. Đang áp dụng thuật toán Levenshtein cho từ khóa: '{}'", queryText);
            List<Laptop> allLaptops = laptopRepository.findByDeleted(false);
            dbLaptops = allLaptops.stream().filter(l -> {
                // Check filters first
                if (brandFilter != null && !l.getBrand().equalsIgnoreCase(brandFilter)) return false;
                if (categoryFilter != null && !l.getCategory().equalsIgnoreCase(categoryFilter)) return false;
                
                try {
                    double p = Double.parseDouble(l.getPrice());
                    if (minPrice != null && p < minPrice) return false;
                    if (maxPrice != null && p > maxPrice) return false;
                } catch (Exception e) {}

                // Check distance
                String pName = l.getName().toLowerCase();
                String qText = queryText.toLowerCase();
                
                // If query is contained or contains, or distance is small
                if (pName.contains(qText)) return true;
                
                String[] tokens = qText.split("\\s+");
                for (String token : tokens) {
                    if (token.length() < 3) continue;
                    for (String word : pName.split("\\s+")) {
                        if (word.length() < 3) continue;
                        int distance = getLevenshteinDistance(token, word);
                        if (distance <= 1) { // 1 typo allowed
                            return true;
                        }
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }

        return dbLaptops;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    private LaptopDocument mapToDocument(Laptop laptop) {
        Double priceVal = 0.0;
        try {
            priceVal = Double.parseDouble(laptop.getPrice());
        } catch (Exception e) {
            log.error("Không thể chuyển đổi giá sản phẩm thành số thực cho ID={} | giá={}", laptop.getId(), laptop.getPrice());
        }

        return LaptopDocument.builder()
                .id(laptop.getId())
                .name(laptop.getName())
                .brand(laptop.getBrand())
                .category(laptop.getCategory())
                .description(laptop.getDescription())
                .shortDescription(laptop.getShortDescription())
                .price(priceVal)
                .images(laptop.getImages())
                .deleted(laptop.isDeleted())
                .build();
    }
}
