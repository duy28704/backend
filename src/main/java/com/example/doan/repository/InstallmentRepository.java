package com.example.doan.repository;

import com.example.doan.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, String> {
    List<Installment> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
