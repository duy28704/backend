package com.example.doan.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {

    // thời gian response
    private LocalDateTime timestamp;

    // status code
    private int status;

    // message
    private String message;

    // data trả về
    private T data;
}
