package com.yowyob.erp.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * Provides API metadata and security scheme for JWT authentication.
 */
@Configuration
public class SwaggerConfig {
    /**
     * Configures the public API group for Swagger documentation.
     * Includes all endpoints under /api/**.
     * @return GroupedOpenApi configuration for public APIs.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }

   
}