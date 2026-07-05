package com.example.doan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest {

    @NotNull(message = "ID sản phẩm không được trống")
    private Long productId;

    @NotBlank(message = "Loại nghiệp vụ không được trống")
    private String type; // IMPORT, EXPORT, ADJUST, AUDIT

    @NotNull(message = "Số lượng không được trống")
    private Integer quantity; // Quantity to import/export or target quantity for audit

    private String note;
}
