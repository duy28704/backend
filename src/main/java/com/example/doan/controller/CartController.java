package com.example.doan.controller;

import com.example.doan.dto.CartItemDto;
import com.example.doan.dto.CartItemRequest;
import com.example.doan.dto.CartItemUpdateRequest;
import com.example.doan.entity.CartItem;
import com.example.doan.entity.User;
import com.example.doan.mapper.CartItemMapper;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartItemMapper cartItemMapper;

    @GetMapping("/get")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> getCart(@AuthenticationPrincipal User user) {
        List<CartItem> items = cartService.getCart(user);
        List<CartItemDto> dtoList = cartItemMapper.toDtoList(items);

        ApiResponse<List<CartItemDto>> response = ApiResponse.<List<CartItemDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Get cart success")
                .data(dtoList)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> addToCart(
            @AuthenticationPrincipal User user,
            @RequestBody CartItemRequest request
    ) {
        List<CartItem> items = cartService.addToCart(user, request.getProductId(), request.getQuantity(), request.getConfiguration());
        List<CartItemDto> dtoList = cartItemMapper.toDtoList(items);

        ApiResponse<List<CartItemDto>> response = ApiResponse.<List<CartItemDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Add to cart success")
                .data(dtoList)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> updateQuantity(
            @AuthenticationPrincipal User user,
            @RequestBody CartItemUpdateRequest request
    ) {
        List<CartItem> items = cartService.updateQuantity(user, request.getProductId(), request.getConfiguration(), request.getQuantity());
        List<CartItemDto> dtoList = cartItemMapper.toDtoList(items);

        ApiResponse<List<CartItemDto>> response = ApiResponse.<List<CartItemDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Update quantity success")
                .data(dtoList)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> removeItem(
            @AuthenticationPrincipal User user,
            @RequestParam Long productId,
            @RequestParam String configuration
    ) {
        List<CartItem> items = cartService.removeItem(user, productId, configuration);
        List<CartItemDto> dtoList = cartItemMapper.toDtoList(items);

        ApiResponse<List<CartItemDto>> response = ApiResponse.<List<CartItemDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Remove item success")
                .data(dtoList)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<List<CartItemDto>>> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        
        ApiResponse<List<CartItemDto>> response = ApiResponse.<List<CartItemDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Clear cart success")
                .data(List.of())
                .build();

        return ResponseEntity.ok(response);
    }
}
