Je vais créer le microservice **loan-management-rest** avec sa propre base de données, suivant l'architecture Database per Service.

# Banking Loan Management Service

## Structure du projet

```
banking-loan-management/
├── src/
│   ├── main/
│   │   ├── java/com/protonmail/landrevillejf/bankingloan/
│   │   │   ├── BankingLoanApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── WebClientConfig.java
│   │   │   │   └── RetryConfig.java
│   │   │   ├── controller/
│   │   │   │   └── LoanController.java
│   │   │   ├── dto/
│   │   │   │   ├── LoanRequestDTO.java
│   │   │   │   ├── LoanResponseDTO.java
│   │   │   │   ├── LoanApplicationRequestDTO.java
│   │   │   │   ├── RepaymentRequestDTO.java
│   │   │   │   └── ErrorResponseDTO.java
│   │   │   ├── entity/
│   │   │   │   ├── Loan.java
│   │   │   │   ├── LoanRepayment.java
│   │   │   │   ├── LoanProduct.java
│   │   │   │   └── LoanStatus.java
│   │   │   ├── repository/
│   │   │   │   ├── LoanRepository.java
│   │   │   │   ├── LoanRepaymentRepository.java
│   │   │   │   └── LoanProductRepository.java
│   │   │   ├── service/
│   │   │   │   ├── LoanService.java
│   │   │   │   └── LoanServiceImpl.java
│   │   │   ├── client/
│   │   │   │   ├── AccountClient.java
│   │   │   │   └── ProfileClient.java
│   │   │   └── exception/
│   │   │       ├── LoanNotFoundException.java
│   │   │       ├── LoanValidationException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_loans_table.sql
│   │           ├── V2__create_loan_repayments_table.sql
│   │           ├── V3__create_loan_products_table.sql
│   │           └── V4__insert_loan_products.sql
│   └── test/
└── build.gradle
```

## build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.14'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'jacoco'
    id 'checkstyle'
}

group = 'com.protonmail.landrevillejf'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core:10.17.0'
    implementation 'org.flywaydb:flyway-database-postgresql:10.17.0'
    
    // Resilience4j
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-retry:2.2.0'
    
    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
    
    // OpenAPI
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // MapStruct
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
    
    // Tests
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}

test {
    useJUnitPlatform()
}
```

## application.yml

```yaml
spring:
  application:
    name: banking-loan-rest

  datasource:
    url: jdbc:postgresql://localhost:5436/loan_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8089
  servlet:
    context-path: /api/v1

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha

logging:
  level:
    com.protonmail.landrevillejf.bankingloan: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

loan:
  interest:
    personal: 12.5
    mortgage: 5.5
    auto: 8.0
    business: 10.0
  max-amount:
    personal: 50000
    mortgage: 1000000
    auto: 75000
    business: 500000
  min-amount: 1000
  max-tenure-months: 360
  min-tenure-months: 6

services:
  account:
    url: ${ACCOUNT_SERVICE_URL:http://localhost:8084}
  profile:
    url: ${PROFILE_SERVICE_URL:http://localhost:8085}
```

## Entités

### Loan.java

```java
package com.protonmail.landrevillejf.bankingloan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String loanReference;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    private LoanProduct loanProduct;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterest;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRepayable;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private String purpose;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime disbursedAt;

    private LocalDateTime closedAt;

    private String approvedBy;

    @Column(precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Version
    private Integer version;
}
```

### LoanProduct.java

```java
package com.protonmail.landrevillejf.bankingloan.entity;

public enum LoanProduct {
    PERSONAL,
    MORTGAGE,
    AUTO,
    BUSINESS
}
```

### LoanStatus.java

```java
package com.protonmail.landrevillejf.bankingloan.entity;

public enum LoanStatus {
    PENDING,
    APPROVED,
    DISBURSED,
    ACTIVE,
    PARTIALLY_PAID,
    CLOSED,
    DEFAULTED,
    REJECTED
}
```

### LoanRepayment.java

```java
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

enum RepaymentStatus {
    PENDING,
    PAID,
    OVERDUE,
    PARTIALLY_PAID
}
```

### LoanProductConfig.java

```java
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
```

## DTOs

### LoanRequestDTO.java

```java
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
```

### LoanResponseDTO.java

```java
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
```

### RepaymentRequestDTO.java

```java
package com.protonmail.landrevillejf.bankingloan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentRequestDTO {

    @NotBlank(message = "Loan reference is required")
    private String loanReference;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String transactionReference;
}
```

## Service

### LoanService.java

```java
package com.protonmail.landrevillejf.bankingloan.service;

import com.protonmail.landrevillejf.bankingloan.dto.LoanRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.LoanResponseDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentResponseDTO;
import com.protonmail.landrevillejf.bankingloan.entity.LoanStatus;

import java.util.List;

public interface LoanService {

    LoanResponseDTO applyForLoan(LoanRequestDTO request);

    LoanResponseDTO approveLoan(String loanReference, String approvedBy);

    LoanResponseDTO disburseLoan(String loanReference);

    LoanResponseDTO getLoanByReference(String loanReference);

    List<LoanResponseDTO> getLoansByAccountNumber(String accountNumber);

    List<LoanResponseDTO> getLoansByCustomerId(String customerId);

    List<LoanResponseDTO> getLoansByStatus(LoanStatus status);

    RepaymentResponseDTO makeRepayment(RepaymentRequestDTO request);

    LoanResponseDTO rejectLoan(String loanReference, String reason);
}
```

### LoanServiceImpl.java

```java
package com.protonmail.landrevillejf.bankingloan.service;

import com.protonmail.landrevillejf.bankingloan.client.AccountClient;
import com.protonmail.landrevillejf.bankingloan.client.ProfileClient;
import com.protonmail.landrevillejf.bankingloan.dto.LoanRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.LoanResponseDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentResponseDTO;
import com.protonmail.landrevillejf.bankingloan.entity.*;
import com.protonmail.landrevillejf.bankingloan.exception.LoanNotFoundException;
import com.protonmail.landrevillejf.bankingloan.exception.LoanValidationException;
import com.protonmail.landrevillejf.bankingloan.repository.LoanProductRepository;
import com.protonmail.landrevillejf.bankingloan.repository.LoanRepaymentRepository;
import com.protonmail.landrevillejf.bankingloan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanProductRepository loanProductRepository;
    private final AccountClient accountClient;
    private final ProfileClient profileClient;

    @Value("${loan.max-amount.personal:50000}")
    private BigDecimal maxPersonalLoan;

    @Value("${loan.max-amount.mortgage:1000000}")
    private BigDecimal maxMortgageLoan;

    @Value("${loan.max-amount.auto:75000}")
    private BigDecimal maxAutoLoan;

    @Value("${loan.max-amount.business:500000}")
    private BigDecimal maxBusinessLoan;

    @Value("${loan.interest.personal:12.5}")
    private BigDecimal personalInterest;

    @Value("${loan.interest.mortgage:5.5}")
    private BigDecimal mortgageInterest;

    @Value("${loan.interest.auto:8.0}")
    private BigDecimal autoInterest;

    @Value("${loan.interest.business:10.0}")
    private BigDecimal businessInterest;

    @Override
    @Transactional
    public LoanResponseDTO applyForLoan(LoanRequestDTO request) {
        log.info("Processing loan application for account: {}", request.getAccountNumber());

        // Validate account exists
        if (!accountClient.accountExists(request.getAccountNumber())) {
            throw new LoanValidationException("Account not found: " + request.getAccountNumber());
        }

        // Get customer profile for risk assessment
        String customerId = accountClient.getCustomerId(request.getAccountNumber());
        String rating = profileClient.getRating(customerId);

        // Validate loan amount based on product
        validateLoanAmount(request.getLoanProduct(), request.getPrincipalAmount());

        // Validate based on customer rating
        validateCustomerRating(rating, request.getPrincipalAmount(), request.getLoanProduct());

        // Calculate loan details
        BigDecimal interestRate = getInterestRate(request.getLoanProduct());
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.getPrincipalAmount(),
                interestRate,
                request.getTenureMonths()
        );
        BigDecimal totalInterest = monthlyPayment
                .multiply(BigDecimal.valueOf(request.getTenureMonths()))
                .subtract(request.getPrincipalAmount());
        BigDecimal totalRepayable = request.getPrincipalAmount().add(totalInterest);

        // Create loan record
        String loanReference = generateLoanReference();

        Loan loan = Loan.builder()
                .loanReference(loanReference)
                .accountNumber(request.getAccountNumber())
                .customerId(customerId)
                .loanProduct(request.getLoanProduct())
                .principalAmount(request.getPrincipalAmount())
                .interestRate(interestRate)
                .totalInterest(totalInterest)
                .totalRepayable(totalRepayable)
                .monthlyPayment(monthlyPayment)
                .tenureMonths(request.getTenureMonths())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(request.getTenureMonths()))
                .status(LoanStatus.PENDING)
                .purpose(request.getPurpose())
                .remainingBalance(totalRepayable)
                .build();

        loan = loanRepository.save(loan);

        // Create repayment schedule
        createRepaymentSchedule(loan);

        log.info("Loan application created with reference: {}", loanReference);
        return toResponseDTO(loan);
    }

    @Override
    @Transactional
    public LoanResponseDTO approveLoan(String loanReference, String approvedBy) {
        log.info("Approving loan: {}", loanReference);

        Loan loan = loanRepository.findByLoanReference(loanReference)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanReference));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanValidationException("Loan cannot be approved. Current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setApprovedBy(approvedBy);
        loan = loanRepository.save(loan);

        return toResponseDTO(loan);
    }

    @Override
    @Transactional
    public LoanResponseDTO disburseLoan(String loanReference) {
        log.info("Disbursing loan: {}", loanReference);

        Loan loan = loanRepository.findByLoanReference(loanReference)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanReference));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new LoanValidationException("Loan cannot be disbursed. Current status: " + loan.getStatus());
        }

        // Credit the account
        accountClient.updateBalance(loan.getAccountNumber(), loan.getPrincipalAmount(), "CREDIT");

        loan.setStatus(LoanStatus.DISBURSED);
        loan.setDisbursedAt(LocalDateTime.now());
        loan = loanRepository.save(loan);

        log.info("Loan disbursed successfully: {}", loanReference);
        return toResponseDTO(loan);
    }

    @Override
    public LoanResponseDTO getLoanByReference(String loanReference) {
        log.info("Getting loan by reference: {}", loanReference);

        Loan loan = loanRepository.findByLoanReference(loanReference)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanReference));

        return toResponseDTO(loan);
    }

    @Override
    public List<LoanResponseDTO> getLoansByAccountNumber(String accountNumber) {
        log.info("Getting loans for account: {}", accountNumber);

        return loanRepository.findByAccountNumber(accountNumber).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanResponseDTO> getLoansByCustomerId(String customerId) {
        log.info("Getting loans for customer: {}", customerId);

        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanResponseDTO> getLoansByStatus(LoanStatus status) {
        log.info("Getting loans by status: {}", status);

        return loanRepository.findByStatus(status).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RepaymentResponseDTO makeRepayment(RepaymentRequestDTO request) {
        log.info("Processing repayment for loan: {}", request.getLoanReference());

        Loan loan = loanRepository.findByLoanReference(request.getLoanReference())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanReference()));

        // Debit the account
        accountClient.updateBalance(loan.getAccountNumber(), request.getAmount(), "DEBIT");

        // Update loan balance
        BigDecimal newBalance = loan.getRemainingBalance().subtract(request.getAmount());
        loan.setRemainingBalance(newBalance.max(BigDecimal.ZERO));

        // Update status
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
        } else if (loan.getStatus() == LoanStatus.DISBURSED || loan.getStatus() == LoanStatus.ACTIVE) {
            loan.setStatus(LoanStatus.ACTIVE);
        }

        loan = loanRepository.save(loan);

        // Find and update repayment installments
        updateRepaymentInstallments(loan.getLoanReference(), request.getAmount());

        return RepaymentResponseDTO.builder()
                .loanReference(request.getLoanReference())
                .amountPaid(request.getAmount())
                .remainingBalance(loan.getRemainingBalance())
                .status(loan.getStatus())
                .transactionDate(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public LoanResponseDTO rejectLoan(String loanReference, String reason) {
        log.info("Rejecting loan: {}", loanReference);

        Loan loan = loanRepository.findByLoanReference(loanReference)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanReference));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanValidationException("Loan cannot be rejected. Current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan = loanRepository.save(loan);

        return toResponseDTO(loan);
    }

    private void validateLoanAmount(LoanProduct product, BigDecimal amount) {
        BigDecimal maxAmount = switch (product) {
            case PERSONAL -> maxPersonalLoan;
            case MORTGAGE -> maxMortgageLoan;
            case AUTO -> maxAutoLoan;
            case BUSINESS -> maxBusinessLoan;
        };

        if (amount.compareTo(maxAmount) > 0) {
            throw new LoanValidationException(
                    String.format("Loan amount %.2f exceeds maximum allowed for %s loans: %.2f",
                            amount, product, maxAmount));
        }
    }

    private void validateCustomerRating(String rating, BigDecimal amount, LoanProduct product) {
        // Higher risk customers get lower loan amounts
        BigDecimal maxAllowed = amount;
        
        switch (rating) {
            case "POOR", "VERY_POOR":
                maxAllowed = amount.multiply(BigDecimal.valueOf(0.5));
                throw new LoanValidationException(
                        "Customer rating " + rating + " does not meet minimum requirements for this loan");
            case "AVERAGE":
                maxAllowed = amount.multiply(BigDecimal.valueOf(0.75));
                break;
            case "GOOD":
                break;
            case "EXCELLENT":
                // Excellent customers get better rates
                break;
        }
    }

    private BigDecimal getInterestRate(LoanProduct product) {
        return switch (product) {
            case PERSONAL -> personalInterest;
            case MORTGAGE -> mortgageInterest;
            case AUTO -> autoInterest;
            case BUSINESS -> businessInterest;
        };
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(BigDecimal.ONE.add(monthlyRate).pow(months), 10, RoundingMode.HALF_UP)
        );
        
        return principal.multiply(monthlyRate)
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void createRepaymentSchedule(Loan loan) {
        BigDecimal monthlyPayment = loan.getMonthlyPayment();
        BigDecimal remainingAmount = loan.getTotalRepayable();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LoanRepayment repayment = LoanRepayment.builder()
                    .loanReference(loan.getLoanReference())
                    .installmentNumber(i)
                    .dueAmount(monthlyPayment)
                    .dueDate(loan.getStartDate().plusMonths(i))
                    .status(RepaymentStatus.PENDING)
                    .build();

            loanRepaymentRepository.save(repayment);
        }
    }

    private void updateRepaymentInstallments(String loanReference, BigDecimal paymentAmount) {
        List<LoanRepayment> pendingRepayments = loanRepaymentRepository
                .findByLoanReferenceAndStatusOrderByInstallmentNumberAsc(loanReference, RepaymentStatus.PENDING);
        
        BigDecimal remaining = paymentAmount;
        
        for (LoanRepayment repayment : pendingRepayments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal dueAmount = repayment.getDueAmount();
            if (remaining.compareTo(dueAmount) >= 0) {
                repayment.setPaidAmount(dueAmount);
                repayment.setStatus(RepaymentStatus.PAID);
                remaining = remaining.subtract(dueAmount);
            } else {
                repayment.setPaidAmount(remaining);
                repayment.setStatus(RepaymentStatus.PARTIALLY_PAID);
                remaining = BigDecimal.ZERO;
            }
            
            repayment.setPaidDate(LocalDate.now());
            loanRepaymentRepository.save(repayment);
        }
        
        // Mark remaining as overdue if any pending after due date
        LocalDate today = LocalDate.now();
        List<LoanRepayment> overdueRepayments = loanRepaymentRepository
                .findByLoanReferenceAndDueDateBeforeAndStatusNot(loanReference, today, RepaymentStatus.PAID);
        
        for (LoanRepayment repayment : overdueRepayments) {
            repayment.setStatus(RepaymentStatus.OVERDUE);
            loanRepaymentRepository.save(repayment);
        }
    }

    private String generateLoanReference() {
        return "LN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private LoanResponseDTO toResponseDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanReference(loan.getLoanReference())
                .accountNumber(loan.getAccountNumber())
                .customerId(loan.getCustomerId())
                .loanProduct(loan.getLoanProduct())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .totalInterest(loan.getTotalInterest())
                .totalRepayable(loan.getTotalRepayable())
                .monthlyPayment(loan.getMonthlyPayment())
                .tenureMonths(loan.getTenureMonths())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .status(loan.getStatus())
                .remainingBalance(loan.getRemainingBalance())
                .createdAt(loan.getCreatedAt())
                .approvedAt(loan.getApprovedAt())
                .disbursedAt(loan.getDisbursedAt())
                .build();
    }
}
```

## Controller

### LoanController.java

```java
package com.protonmail.landrevillejf.bankingloan.controller;

import com.protonmail.landrevillejf.bankingloan.dto.LoanRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.LoanResponseDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentRequestDTO;
import com.protonmail.landrevillejf.bankingloan.dto.RepaymentResponseDTO;
import com.protonmail.landrevillejf.bankingloan.entity.LoanStatus;
import com.protonmail.landrevillejf.bankingloan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loan Management", description = "API for managing bank loans")
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Loan Management Service is running");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<LoanResponseDTO> applyForLoan(@Valid @RequestBody LoanRequestDTO request) {
        log.info("Applying for loan for account: {}", request.getAccountNumber());
        LoanResponseDTO response = loanService.applyForLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{loanReference}/approve")
    @Operation(summary = "Approve a loan")
    public ResponseEntity<LoanResponseDTO> approveLoan(
            @PathVariable String loanReference,
            @RequestParam String approvedBy) {
        log.info("Approving loan: {}", loanReference);
        LoanResponseDTO response = loanService.approveLoan(loanReference, approvedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanReference}/disburse")
    @Operation(summary = "Disburse approved loan")
    public ResponseEntity<LoanResponseDTO> disburseLoan(@PathVariable String loanReference) {
        log.info("Disbursing loan: {}", loanReference);
        LoanResponseDTO response = loanService.disburseLoan(loanReference);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanReference}/reject")
    @Operation(summary = "Reject a loan application")
    public ResponseEntity<LoanResponseDTO> rejectLoan(
            @PathVariable String loanReference,
            @RequestParam String reason) {
        log.info("Rejecting loan: {}", loanReference);
        LoanResponseDTO response = loanService.rejectLoan(loanReference, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{loanReference}")
    @Operation(summary = "Get loan by reference")
    public ResponseEntity<LoanResponseDTO> getLoanByReference(@PathVariable String loanReference) {
        log.info("Getting loan: {}", loanReference);
        LoanResponseDTO response = loanService.getLoanByReference(loanReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get loans by account number")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByAccountNumber(@PathVariable String accountNumber) {
        log.info("Getting loans for account: {}", accountNumber);
        List<LoanResponseDTO> response = loanService.getLoansByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get loans by customer ID")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByCustomerId(@PathVariable String customerId) {
        log.info("Getting loans for customer: {}", customerId);
        List<LoanResponseDTO> response = loanService.getLoansByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get loans by status")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByStatus(@PathVariable LoanStatus status) {
        log.info("Getting loans by status: {}", status);
        List<LoanResponseDTO> response = loanService.getLoansByStatus(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/repay")
    @Operation(summary = "Make a loan repayment")
    public ResponseEntity<RepaymentResponseDTO> makeRepayment(@Valid @RequestBody RepaymentRequestDTO request) {
        log.info("Making repayment for loan: {}", request.getLoanReference());
        RepaymentResponseDTO response = loanService.makeRepayment(request);
        return ResponseEntity.ok(response);
    }
}
```

## Flyway Scripts

### V1__create_loans_table.sql

```sql
CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY,
    loan_reference VARCHAR(50) UNIQUE NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    loan_product VARCHAR(20) NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    total_interest DECIMAL(19,2) NOT NULL,
    total_repayable DECIMAL(19,2) NOT NULL,
    monthly_payment DECIMAL(19,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    purpose TEXT,
    remaining_balance DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    disbursed_at TIMESTAMP,
    closed_at TIMESTAMP,
    approved_by VARCHAR(100),
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_loans_account_number ON loans(account_number);
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_loan_reference ON loans(loan_reference);
```

### V2__create_loan_repayments_table.sql

```sql
CREATE TABLE IF NOT EXISTS loan_repayments (
    id UUID PRIMARY KEY,
    loan_reference VARCHAR(50) NOT NULL,
    installment_number INTEGER NOT NULL,
    due_amount DECIMAL(19,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_amount DECIMAL(19,2),
    paid_date DATE,
    status VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (loan_reference) REFERENCES loans(loan_reference)
);

CREATE INDEX idx_repayments_loan_reference ON loan_repayments(loan_reference);
CREATE INDEX idx_repayments_status ON loan_repayments(status);
CREATE INDEX idx_repayments_due_date ON loan_repayments(due_date);
```

### V3__create_loan_products_table.sql

```sql
CREATE TABLE IF NOT EXISTS loan_products (
    id BIGSERIAL PRIMARY KEY,
    product_type VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(50) NOT NULL,
    default_interest_rate DECIMAL(5,2) NOT NULL,
    max_amount DECIMAL(19,2) NOT NULL,
    min_amount DECIMAL(19,2) NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    min_tenure_months INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true
);
```

### V4__insert_loan_products.sql

```sql
INSERT INTO loan_products (product_type, product_name, default_interest_rate, max_amount, min_amount, max_tenure_months, min_tenure_months)
VALUES 
    ('PERSONAL', 'Personal Loan', 12.5, 50000, 1000, 60, 12),
    ('MORTGAGE', 'Mortgage Loan', 5.5, 1000000, 50000, 360, 60),
    ('AUTO', 'Auto Loan', 8.0, 75000, 5000, 72, 12),
    ('BUSINESS', 'Business Loan', 10.0, 500000, 10000, 120, 12)
ON CONFLICT (product_type) DO UPDATE SET
    product_name = EXCLUDED.product_name,
    default_interest_rate = EXCLUDED.default_interest_rate,
    max_amount = EXCLUDED.max_amount,
    min_amount = EXCLUDED.min_amount,
    max_tenure_months = EXCLUDED.max_tenure_months,
    min_tenure_months = EXCLUDED.min_tenure_months;
```

## docker-compose.yml (ajout)

```yaml
postgres-loan:
  image: postgres:15-alpine
  container_name: loan-postgres
  environment:
    POSTGRES_DB: loan_db
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: password
  ports:
    - "5436:5432"
  volumes:
    - loan_data:/var/lib/postgresql/data
  networks:
    - banking-network
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 10s
    timeout: 5s
    retries: 5

loan-service:
  build:
    context: ../banking-loan-management
    dockerfile: Dockerfile
  container_name: loan-service
  ports:
    - "8089:8089"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-loan:5432/loan_db
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: password
    SERVICES_ACCOUNT_URL: http://account-service:8084
    SERVICES_PROFILE_URL: http://profile-service:8085
  depends_on:
    postgres-loan:
      condition: service_healthy
    postgres-account:
      condition: service_healthy
    postgres-profile:
      condition: service_healthy
  networks:
    - banking-network

volumes:
  loan_data:

networks:
  banking-network:
    driver: bridge
```

## Résumé

| Service | Port | Base de Données |
|---------|------|-----------------|
| loan-management-rest | 8089 | loan_db (5436) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/api/loans/apply` | Apply for a loan |
| POST | `/api/v1/api/loans/{reference}/approve` | Approve loan |
| POST | `/api/v1/api/loans/{reference}/disburse` | Disburse loan |
| POST | `/api/v1/api/loans/{reference}/reject` | Reject loan |
| GET | `/api/v1/api/loans/{reference}` | Get loan by reference |
| GET | `/api/v1/api/loans/account/{accountNumber}` | Get loans by account |
| GET | `/api/v1/api/loans/customer/{customerId}` | Get loans by customer |
| GET | `/api/v1/api/loans/status/{status}` | Get loans by status |
| POST | `/api/v1/api/loans/repay` | Make repayment |

## Swagger UI

```
http://localhost:8089/api/v1/swagger-ui.html
```