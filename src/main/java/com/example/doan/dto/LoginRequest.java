package com.example.doan.dto;

import lombok.Data;

@Data
public class LoginRequest {

    // username login
    private String username;

    // email login
    private String email;

    // password login
    private String password;

    private String captchaId;
    private String captchaAnswer;
    private String turnstileToken;
}
