package com.protonmail.landrevillejf.bankingloan.repository;

import com.protonmail.landrevillejf.bankingloan.entity.Loan;
import com.protonmail.landrevillejf.bankingloan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    Optional<Loan> findByLoanReference(String loanReference);

    List<Loan> findByAccountNumber(String accountNumber);

    List<Loan> findByCustomerId(String customerId);

    List<Loan> findByStatus(LoanStatus status);
}
