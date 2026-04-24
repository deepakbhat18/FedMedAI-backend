package com.fedlearn.medical.config;

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
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Federated Learning Medical Diagnosis API")
                        .description(
                                "REST API for FL-based Medical Diagnosis System.\n\n"
                                        + "**How to authenticate:**\n"
                                        + "1. Use POST /api/auth/login with credentials\n"
                                        + "2. Copy the accessToken from response\n"
                                        + "3. Click Authorize button above\n"
                                        + "4. Paste token and click Authorize\n\n"
                                        + "**Default Credentials:**\n"
                                        + "- Admin: admin / admin123\n"
                                        + "- Hospital1: hospital1_admin / hospital123\n"
                                        + "- Hospital2: hospital2_admin / hospital123\n"
                                        + "- Hospital3: hospital3_admin / hospital123"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FL Medical Team")
                                .email("team@flmedical.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token here. "
                                                + "Get it from POST /api/auth/login")));
    }
}