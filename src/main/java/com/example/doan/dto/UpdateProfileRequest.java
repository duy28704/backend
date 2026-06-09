package com.example.doan.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String address;
    private String dob;
    private String gender;
    private String avatarUrl;
}
