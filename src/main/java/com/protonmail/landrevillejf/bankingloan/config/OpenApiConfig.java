package com.protonmail.landrevillejf.bankingloan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Loan Management API")
                        .version("1.0")
                        .description("API for managing bank loans")
                        .contact(new Contact()
                                .name("Banking Team")
                                .email("support@banking.com")));
    }
}
