package com.yowyob.erp.config.auth;

import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final AuthService authService;

    @Value("${app.organization.header-name:X-Tenant-ID}")
    private String organizationHeaderName;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token != null) {
            boolean hasExplicitTenant = hasExplicitTenant(exchange);
            return authService.validateToken(token)
                    .flatMap(authResponse -> {
                        if (!authResponse.isValid()) {
                            return chain.filter(exchange);
                        }
                        UsernamePasswordAuthenticationToken authentication = createAuthentication(authResponse);
                        Mono<Void> downstream = chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

                        // Derive the tenant from the authenticated user's organization,
                        // unless the request carries an explicit X-Tenant-ID header /
                        // tenantId param (which keeps precedence for cross-org access).
                        UUID organizationId = parseOrganizationId(authResponse.getOrganizationId());
                        if (!hasExplicitTenant && organizationId != null) {
                            downstream = downstream.contextWrite(ctx ->
                                    ctx.put(ReactiveOrganizationContext.ORGANIZATION_ID_KEY, organizationId));
                        }
                        return downstream;
                    })
                    .onErrorResume(error -> {
                         log.error("Error validating token", error);
                         return chain.filter(exchange);
                    });
        }

        return chain.filter(exchange);
    }

    /** True when the request explicitly targets a tenant via header or query param. */
    private boolean hasExplicitTenant(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(organizationHeaderName);
        if (header != null && !header.isEmpty()) {
            return true;
        }
        String queryParam = exchange.getRequest().getQueryParams().getFirst("tenantId");
        return queryParam != null && !queryParam.isEmpty();
    }

    private UUID parseOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            log.warn("Authenticated user has a non-UUID organizationId: {}", organizationId);
            return null;
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null; // Return null if not found or invalid format
    }

    private UsernamePasswordAuthenticationToken createAuthentication(AuthValidationResponse authResponse) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(authResponse.getRoles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(authResponse.getUserId(), null, authorities);
    }
}