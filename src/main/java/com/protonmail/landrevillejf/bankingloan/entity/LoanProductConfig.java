package com.protonmail.landrevillejf.bankingloan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LoanProduct productType;

    private String productName;

    private BigDecimal defaultInterestRate;

    private BigDecimal maxAmount;

    private BigDecimal minAmount;

    private Integer maxTenureMonths;

    private Integer minTenureMonths;

    private Boolean isActive;
}
