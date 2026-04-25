package com.protonmail.landrevillejf.bankingloan.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.profile.url:http://localhost:8085}")
    private String profileServiceUrl;

    public String getRating(String customerId) {
        // Mock implementation - in real scenario, call profile service
        log.info("Getting rating for customer: {}", customerId);
        return "GOOD"; // Mock rating
    }
}
