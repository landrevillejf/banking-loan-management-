package com.protonmail.landrevillejf.bankingloan.dto;

import com.protonmail.landrevillejf.bankingloan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentResponseDTO {

    private String loanReference;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;
    private LoanStatus status;
    private LocalDateTime transactionDate;
}
