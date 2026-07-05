package com.example.doan.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    // access token JWT
    private String accessToken;

    // refresh token
    private String refreshToken;

    // role user
    private String role;

    // permissions user
    private java.util.List<String> permissions;

    // username
    private String username;

    // email
    private String email;

    // profile fields
    private String name;
    private String phone;
    private String address;
    private String dob;
    private String gender;
    private String avatarUrl;
    private String joinedDate;

    // login message
    private String message;
}
