package com.example.doan.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {

    // thời gian lỗi
    private LocalDateTime timestamp;

    // status code
    private int status;

    // message lỗi
    private String message;

    // validation errors
    private Map<String, String> errors;
}
