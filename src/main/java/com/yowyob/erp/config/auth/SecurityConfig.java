// Security configuration with JWT for WebFlux
package com.yowyob.erp.config.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * Reactive Security configuration class.
 * Configures JWT filter, CORS, and request authorization for WebFlux.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Constructor for SecurityConfig.
     * 
     * @param jwtAuthenticationFilter the JWT filter (must be a WebFilter)
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the reactive security filter chain.
     * 
     * @param http the ServerHttpSecurity object
     * @return the SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Wire CORS into the security chain (uses the CorsConfigurationSource bean
                // from CorsConfig). This applies CORS at SecurityWebFiltersOrder.CORS, before
                // authentication, so error responses (401/400) also carry CORS headers —
                // otherwise the browser masks them as "Failed to fetch".
                .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Return a plain 401 (no "WWW-Authenticate: Basic"), otherwise the
                // browser shows a native Basic-auth popup on protected endpoints.
                .exceptionHandling(spec -> spec.authenticationEntryPoint(
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(exchanges -> exchanges
                        // Preflight CORS must succeed before JWT-protected routes
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Kernel reverse-proxy (BFF): le backend injecte X-Client-Id/X-Api-Key et
                        // relaie le Bearer de l'utilisateur ; c'est le Kernel qui authentifie. On
                        // n'exige donc pas l'auth côté backend (et certains appels sont pré-login,
                        // ex. /api/auth/login). Doit précéder la règle "/api/**".
                        .pathMatchers("/api/kernel/**").permitAll()
                        .pathMatchers("/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/api/public/**",
                                "/api/health",
                                "/favicon.ico")
                        .permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/business/**").hasAnyRole("BusinessActor", "SuperAdmin")
                        // Enforce authentication on all accounting endpoints (enables roles and method-level @PreAuthorize)
                        .pathMatchers("/api/accounting/**").authenticated()
                        .pathMatchers("/api/common/attachments/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
