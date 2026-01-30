package com.yowyob.erp.config.tenant;

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
 * WebFilter to extract the Tenant ID from the request header and put it in the
 * Reactor Context.
 */
@Component
@Order(-101) // Run before security filters
@Slf4j
public class TenantWebFilter implements WebFilter {

    @Value("${app.tenant.header-name:X-Tenant-ID}")
    private String tenantHeaderName;

    @Value("${app.tenant.default-tenant:550e8400-e29b-41d4-a716-446655440000}")
    private String defaultTenantId;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tenantIdStr = request.getHeaders().getFirst(tenantHeaderName);

        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            tenantIdStr = defaultTenantId;
        }

        try {
            UUID tenantId = UUID.fromString(tenantIdStr);
            log.info("Tenant context resolved: {}", tenantId);

            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(ReactiveTenantContext.TENANT_ID_KEY, tenantId));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Tenant ID format: {}", tenantIdStr);
            return chain.filter(exchange);
        }
    }
}
