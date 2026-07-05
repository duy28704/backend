package com.example.doan.repository;

import com.example.doan.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT s.queryText as keyword, COUNT(s) as count FROM SearchLog s GROUP BY s.queryText ORDER BY COUNT(s) DESC")
    List<Map<String, Object>> findTopKeywords(Pageable pageable);

    @Query("SELECT s.queryText as keyword, COUNT(s) as count FROM SearchLog s WHERE s.resultCount = 0 GROUP BY s.queryText ORDER BY COUNT(s) DESC")
    List<Map<String, Object>> findZeroResultKeywords(Pageable pageable);

    @Query("SELECT DATE(s.timestamp) as searchDate, COUNT(s) as searchCount FROM SearchLog s GROUP BY DATE(s.timestamp) ORDER BY DATE(s.timestamp) ASC")
    List<Map<String, Object>> findSearchVolumeOverTime();
}
