package com.fintech.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Access Swagger UI at:     http://localhost:8080/swagger-ui/index.html
// Access raw spec at:       http://localhost:8080/v3/api-docs

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .description("""
                                Production-grade financial records management API with role-based access control.
                                
                                **Roles:**
                                - `VIEWER`  — Read-only access to records and basic dashboard summary
                                - `ANALYST` — All VIEWER permissions + monthly trend analytics
                                - `ADMIN`   — Full access including user management and record mutation
                                
                                **Authentication:** Use POST /api/v1/auth/login to obtain a Bearer token,
                                then click 'Authorize' and enter: `Bearer <your-token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Finance Dashboard Team")
                                .email("api@finance-dashboard.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token (without 'Bearer ' prefix)")));
    }
}