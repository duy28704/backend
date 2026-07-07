package com.example.doan.controller;

import com.example.doan.dto.NotificationDTO;
import com.example.doan.response.ApiResponse;
import com.example.doan.service.NotificationService;
import com.example.doan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    private String extractUsername(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            return jwtUtil.extractUsername(tokenHeader.substring(7));
        }
        throw new RuntimeException("Xác thực không hợp lệ");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getUserNotifications(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String username = extractUsername(tokenHeader);
        Page<NotificationDTO> data = notificationService.getUserNotifications(username, unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<NotificationDTO>>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestHeader("Authorization") String tokenHeader
    ) {
        String username = extractUsername(tokenHeader);
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .status(200)
                .message("Success")
                .data(Map.of("unreadCount", count))
                .build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(
            @RequestHeader("Authorization") String tokenHeader,
            @PathVariable String id
    ) {
        String username = extractUsername(tokenHeader);
        NotificationDTO dto = notificationService.markAsRead(id, username);
        return ResponseEntity.ok(ApiResponse.<NotificationDTO>builder()
                .status(200)
                .message("Mark as read success")
                .data(dto)
                .build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestHeader("Authorization") String tokenHeader
    ) {
        String username = extractUsername(tokenHeader);
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(200)
                .message("Mark all as read success")
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @RequestHeader("Authorization") String tokenHeader,
            @PathVariable String id
    ) {
        String username = extractUsername(tokenHeader);
        notificationService.deleteNotification(id, username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(200)
                .message("Delete notification success")
                .build());
    }
}
