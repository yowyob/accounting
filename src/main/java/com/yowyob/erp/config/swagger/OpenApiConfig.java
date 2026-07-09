// Swagger/OpenAPI Configuration
package com.yowyob.erp.config.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;

import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .addServersItem(new Server().url("https://accounting.yowyob.com/accounting-api")
                                                .description("Production Server"))
                                .addServersItem(new Server().url("http://localhost:8081")
                                                .description("Local Development Server"))
                                .info(new Info()
                                                .title("Yowyob ERP - Accounting API")
                                                .description("REST API for OHADA compliant accounting module")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("AZANGUE LEONEL")
                                                                .email("azangueleonel9@gmail.com"))
                                                .license(new License()
                                                                .name("Owner")
                                                                .url("https://yowyob.com/license")))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("Bearer Authentication")
                                                .addList("X-Tenant-ID"))
                                .components(new Components()
                                                .addSecuritySchemes("Bearer Authentication",
                                                                new SecurityScheme()
                                                                                .type(Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("JWT Token for authentication"))
                                                .addSecuritySchemes("X-Tenant-ID",
                                                                new SecurityScheme()
                                                                                .type(Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Tenant-ID")
                                                                                .description("Tenant/Organization ID for multi-tenancy (UUID format)")));
        }
}
