package com.protonmail.landrevillejf.bankingloan.repository;

import com.protonmail.landrevillejf.bankingloan.entity.LoanProduct;
import com.protonmail.landrevillejf.bankingloan.entity.LoanProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProductConfig, Long> {

    Optional<LoanProductConfig> findByProductType(LoanProduct productType);
}
