package com.protonmail.landrevillejf.bankingloan.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.account.url:http://localhost:8084}")
    private String accountServiceUrl;

    public boolean accountExists(String accountNumber) {
        // Mock implementation - in real scenario, call account service
        log.info("Checking if account exists: {}", accountNumber);
        return true; // Assume exists for now
    }

    public String getCustomerId(String accountNumber) {
        // Mock implementation
        log.info("Getting customer ID for account: {}", accountNumber);
        return "CUST-" + accountNumber; // Mock customer ID
    }

    public void updateBalance(String accountNumber, BigDecimal amount, String transactionType) {
        // Mock implementation - in real scenario, call account service
        log.info("Updating balance for account {}: {} {}", accountNumber, transactionType, amount);
    }
}
