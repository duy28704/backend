package com.example.doan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchStatDto {
    private long totalSearches;
    private List<Map<String, Object>> topKeywords;
    private List<Map<String, Object>> zeroResultKeywords;
    private List<Map<String, Object>> searchVolumeOverTime;
}
