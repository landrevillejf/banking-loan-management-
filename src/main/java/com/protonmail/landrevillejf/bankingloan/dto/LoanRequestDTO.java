package com.protonmail.landrevillejf.bankingloan.dto;

import com.protonmail.landrevillejf.bankingloan.entity.LoanProduct;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequestDTO {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Loan product is required")
    private LoanProduct loanProduct;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000", message = "Principal must be at least 1000")
    @DecimalMax(value = "1000000", message = "Principal cannot exceed 1,000,000")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer tenureMonths;

    private String purpose;
}
