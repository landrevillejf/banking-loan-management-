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
