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
