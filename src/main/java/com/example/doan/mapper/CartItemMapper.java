package com.example.doan.mapper;

import com.example.doan.dto.CartItemDto;
import com.example.doan.entity.CartItem;

import java.util.List;

public interface CartItemMapper {
    CartItemDto toDto(CartItem item);
    List<CartItemDto> toDtoList(List<CartItem> items);
}
