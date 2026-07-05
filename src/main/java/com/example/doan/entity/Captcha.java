package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "captchas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Captcha {
    @Id
    private String id;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false)
    private Instant expiry;
}
