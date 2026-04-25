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
