package com.foodorder.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8083");
        localServer.setDescription("Order Service - Local Development");

        Server gatewayServer = new Server();
        gatewayServer.setUrl("http://localhost:8080/api/orders");
        gatewayServer.setDescription("Order Service - Via API Gateway");

        Contact contact = new Contact();
        contact.setEmail("support@foodordering.com");
        contact.setName("Food Ordering Team");
        contact.setUrl("https://www.foodordering.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Order Service API - Food Ordering System")
                .version("1.0.0")
                .contact(contact)
                .description("Order Management Service API. " +
                        "Handles order creation, status tracking, and initiates the payment saga pattern. " +
                        "Orders transition through PENDING → CONFIRMED/CANCELLED states based on payment results.")
                .termsOfService("https://www.foodordering.com/terms")
                .license(mitLicense);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Authentication - Required for all order operations");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, gatewayServer))
                .components(new Components().addSecuritySchemes("Bearer Authentication", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
