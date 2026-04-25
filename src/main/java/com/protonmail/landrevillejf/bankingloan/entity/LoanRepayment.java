package com.protonmail.landrevillejf.bankingloan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_repayments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String loanReference;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dueAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private RepaymentStatus status;

    private String transactionReference;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
