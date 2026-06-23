package com.example.doan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemUpdateRequest {
    private Long productId;
    private String configuration;
    private Integer quantity;
}
