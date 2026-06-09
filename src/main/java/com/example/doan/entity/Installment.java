package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "installments")
public class Installment {
    @Id
    private String id; // format INS-xxxxx

    private String email;
    private String customerName;
    private String phone;
    private String productId;
    private String productName;
    private Double productPrice;
    private String productImage;
    private Integer downPaymentPct;
    private Double downPaymentAmount;
    private Double loanAmount;
    private Integer loanTerm;
    private Double monthlyPayment;
    private String createdDate;
    private String status;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
