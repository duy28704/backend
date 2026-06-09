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
}