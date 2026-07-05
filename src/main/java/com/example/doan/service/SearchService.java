package com.example.doan.service;

import com.example.doan.dto.SearchStatDto;
import com.example.doan.entity.Laptop;

import java.util.List;

public interface SearchService {
    void indexProduct(Laptop laptop);
    void deleteProduct(Long id);
    void syncAll();
    List<Laptop> search(String queryText, Double minPrice, Double maxPrice, String brand, String category, Boolean autocomplete, Boolean fuzzy, String clientIp);
    SearchStatDto getSearchStatistics();
}
