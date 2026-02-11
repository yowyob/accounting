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
 * WebFilter to extract the Organization ID from the request header and put it in the
 * Reactor Context.
 */
@Component
@Order(-101) // Run before security filters
@Slf4j
public class OrganizationWebFilter implements WebFilter {

    @Value("${app.organization.header-name:X-Organization-ID}")
    private String organizationHeaderName;

    @Value("${app.organization.default-organization:550e8400-e29b-41d4-a716-446655440000}")
    private String defaultOrganizationId;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String organizationIdStr = request.getHeaders().getFirst(organizationHeaderName);

        if (organizationIdStr == null || organizationIdStr.isEmpty()) {
            organizationIdStr = defaultOrganizationId;
        }

        try {
            UUID organizationId = UUID.fromString(organizationIdStr);
            log.info("Organization context resolved: {}", organizationId);

            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(ReactiveOrganizationContext.ORGANIZATION_ID_KEY, organizationId));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Organization ID format: {}", organizationIdStr);
            return chain.filter(exchange);
        }
    }
}
