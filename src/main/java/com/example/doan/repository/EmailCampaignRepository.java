package com.example.doan.repository;

import com.example.doan.entity.EmailCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {
    Page<EmailCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
