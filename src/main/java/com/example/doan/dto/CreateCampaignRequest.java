package com.example.doan.dto;

import lombok.Data;

@Data
public class CreateCampaignRequest {
    private String name;
    private String templateCode;
    private String targetGroup; // ALL_CUSTOMERS, VIP_CUSTOMERS, INACTIVE_30_DAYS
}
