package com.example.doan.repository;


import com.example.doan.entity.Laptop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface LaptopRepository extends JpaRepository<Laptop, Long> {

    List<Laptop> findByDeleted(boolean deleted);

    List<Laptop> findByIdIn(Collection<Long> ids);

    Optional<Laptop> findByLink(String link);

    Optional<Laptop> findByNameIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Laptop l WHERE l.deleted = false AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:brand IS NULL OR :brand = '' OR LOWER(l.brand) = LOWER(:brand)) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(l.category) = LOWER(:category))")
    List<Laptop> searchLaptopsWithFilters(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("brand") String brand,
            @org.springframework.data.repository.query.Param("category") String category
    );
}