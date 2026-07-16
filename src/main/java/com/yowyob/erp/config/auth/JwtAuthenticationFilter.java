package com.yowyob.erp.config.auth;

import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Authenticates the request from the kernel-issued JWT and binds the <b>tenant</b> derived from the
 * token's {@code tid} claim to the reactive context (as a fallback when no {@code X-Tenant-Id}
 * header is present — {@link com.yowyob.erp.config.organization.OrganizationWebFilter} takes
 * precedence).
 *
 * <p>This filter no longer resolves the <b>organization</b>: organization is a per-request concept
 * carried by the {@code X-Organization-Id} header (the kernel JWT has no organization claim), and is
 * resolved solely by {@code OrganizationWebFilter}. Conflating the tenant header with the
 * organization is exactly the bug this change removes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final AuthService authService;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token != null) {
            return authService.validateToken(token)
                    .flatMap(authResponse -> {
                        if (!authResponse.isValid()) {
                            return chain.filter(exchange);
                        }
                        UsernamePasswordAuthenticationToken authentication = createAuthentication(authResponse);
                        Mono<Void> downstream = chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

                        // Bind the tenant from the JWT `tid` claim as a fallback. The outer
                        // OrganizationWebFilter overrides this with the X-Tenant-Id header when present.
                        UUID tenantId = parseUuid(authResponse.getTenantId());
                        if (tenantId != null) {
                            downstream = downstream.contextWrite(ctx ->
                                    ctx.put(ReactiveOrganizationContext.TENANT_ID_KEY, tenantId));
                        }
                        String userId = authResponse.getUserId();
                        if (userId != null && !userId.isBlank()) {
                            downstream = downstream.contextWrite(ctx ->
                                    ctx.put(ReactiveOrganizationContext.USER_ID_KEY, userId));
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

    private UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.warn("JWT carries a non-UUID tenant id: {}", value);
            return null;
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // EventSource (SSE) cannot send custom headers — token is passed as query param
        String queryToken = exchange.getRequest().getQueryParams().getFirst("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken createAuthentication(AuthValidationResponse authResponse) {
        // Le claim `permissions` du Kernel mêle des noms de rôle nus (ex. RESPONSABLE_COMPTABLE)
        // et des entrées déjà préfixées (ex. ROLE_OWNER, ROLE_ORGANIZATION_ADMIN). On ne préfixe
        // `ROLE_` QUE si l'entrée ne l'est pas déjà — sinon `ROLE_OWNER` deviendrait `ROLE_ROLE_OWNER`
        // et `hasRole('OWNER')` ne matcherait jamais.
        List<SimpleGrantedAuthority> authorities = Arrays.stream(authResponse.getRoles())
                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(authResponse.getUserId(), null, authorities);
    }
}
