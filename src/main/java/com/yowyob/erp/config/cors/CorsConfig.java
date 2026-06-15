package com.yowyob.erp.config.cors;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Reactive CORS configuration source.
     *
     * Exposed as a {@link CorsConfigurationSource} (not a CorsWebFilter) so it is wired
     * INTO the Spring Security filter chain via {@code http.cors()} at
     * {@code SecurityWebFiltersOrder.CORS}. This applies CORS before authentication, so
     * even error responses (401/400) carry Access-Control-Allow-Origin. A standalone
     * CorsWebFilter bean runs after the security chain has already committed the 401
     * (security chain order -100 < filter order 0), leaving error responses without CORS
     * headers — the browser then masks them as "Failed to fetch".
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Parse comma-separated list of allowed origins from env var
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        corsConfiguration.setAllowedOrigins(origins);
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
