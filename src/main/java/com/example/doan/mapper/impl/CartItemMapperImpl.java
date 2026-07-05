package com.example.doan.mapper.impl;

import com.example.doan.dto.CartItemDto;
import com.example.doan.entity.CartItem;
import com.example.doan.mapper.CartItemMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartItemMapperImpl implements CartItemMapper {

    @Override
    public CartItemDto toDto(CartItem item) {
        if (item == null) {
            return null;
        }
        return CartItemDto.builder()
                .id(item.getProduct().getId())
                .quantity(item.getQuantity())
                .configuration(item.getConfiguration())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    @Override
    public List<CartItemDto> toDtoList(List<CartItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
