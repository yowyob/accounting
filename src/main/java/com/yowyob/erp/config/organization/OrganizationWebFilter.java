package com.yowyob.erp.config.organization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFilter to extract the Organization/Tenant ID from:
 *  1. The request header (X-Tenant-ID) — standard API calls
 *  2. The "tenantId" query parameter — SSE stream (EventSource cannot send custom headers)
 * Falls back to the configured default if neither is present.
 */
@Component
@Order(-101) // Run before security filters
@Slf4j
public class OrganizationWebFilter implements WebFilter {

    @Value("${app.organization.header-name:X-Tenant-ID}")
    private String organizationHeaderName;

    @Value("${app.organization.default-organization:550e8400-e29b-41d4-a716-446655440000}")
    private String defaultOrganizationId;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Try header first (standard API calls)
        String organizationIdStr = request.getHeaders().getFirst(organizationHeaderName);

        // 2. Fallback to query param (SSE connections from EventSource)
        if (organizationIdStr == null || organizationIdStr.isEmpty()) {
            organizationIdStr = request.getQueryParams().getFirst("tenantId");
        }

        // 3. Fallback to configured default
        if (organizationIdStr == null || organizationIdStr.isEmpty()) {
            organizationIdStr = defaultOrganizationId;
        }

        try {
            UUID organizationId = UUID.fromString(organizationIdStr);
            log.debug("Tenant context resolved: {}", organizationId);

            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(ReactiveOrganizationContext.ORGANIZATION_ID_KEY, organizationId));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Organization/Tenant ID format: {}", organizationIdStr);
            return chain.filter(exchange);
        }
    }
}
