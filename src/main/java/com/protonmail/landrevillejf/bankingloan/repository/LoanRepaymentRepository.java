package com.protonmail.landrevillejf.bankingloan.repository;

import com.protonmail.landrevillejf.bankingloan.entity.LoanRepayment;
import com.protonmail.landrevillejf.bankingloan.entity.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    List<LoanRepayment> findByLoanReferenceAndStatusOrderByInstallmentNumberAsc(String loanReference, RepaymentStatus status);

    List<LoanRepayment> findByLoanReferenceAndDueDateBeforeAndStatusNot(String loanReference, LocalDate date, RepaymentStatus status);
}
