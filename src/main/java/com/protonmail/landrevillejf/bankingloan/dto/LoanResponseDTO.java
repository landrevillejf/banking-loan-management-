package com.protonmail.landrevillejf.bankingloan.dto;

import com.protonmail.landrevillejf.bankingloan.entity.LoanProduct;
import com.protonmail.landrevillejf.bankingloan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {

    private String id;
    private String loanReference;
    private String accountNumber;
    private String customerId;
    private LoanProduct loanProduct;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayable;
    private BigDecimal monthlyPayment;
    private Integer tenureMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private LoanStatus status;
    private BigDecimal remainingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
}
