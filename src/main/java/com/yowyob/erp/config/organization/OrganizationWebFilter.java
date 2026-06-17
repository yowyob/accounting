package com.yowyob.erp.config.organization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.UUID;

/**
 * WebFilter that binds the <b>tenant</b> and the <b>organization</b> to the reactive context,
 * aligned with the kernel's contract (they are NOT the same thing):
 * <ul>
 *   <li><b>Organization</b> ({@code X-Organization-Id} header, or {@code organizationId} query
 *       param for SSE since EventSource cannot send custom headers): the business unit that owns
 *       the data (accounts, entries, drafts…).</li>
 *   <li><b>Tenant</b> ({@code X-Tenant-Id} header): the platform customer the organization belongs
 *       to. Also carried by the kernel JWT {@code tid} claim.</li>
 * </ul>
 *
 * <p>When no organization can be resolved, the behaviour depends on
 * {@code app.organization.require-explicit}: when {@code true} (default) the request is rejected
 * with 400 to avoid silently attributing data to the wrong organization (cross-org leak); when
 * {@code false} it falls back to {@code app.organization.default-organization} (bootstrap/dev only).
 * Infrastructure paths (auth, health, docs, JWKS) are always exempt from the requirement.
 */
@Component
@Order(-101) // Run before security filters
@Slf4j
public class OrganizationWebFilter implements WebFilter {

    @Value("${app.organization.org-header-name:X-Organization-Id}")
    private String orgHeaderName;

    @Value("${app.organization.tenant-header-name:X-Tenant-Id}")
    private String tenantHeaderName;

    @Value("${app.organization.default-organization:4e177ff2-89b8-4d24-926a-5763dfa1b19a}")
    private String defaultOrganizationId;

    /**
     * Fail-closed when no organization is resolved (recommended). Set to {@code false} only for
     * local bootstrap/seed flows that rely on the default organization.
     */
    @Value("${app.organization.require-explicit:true}")
    private boolean requireExplicit;

    /** Paths that never require an organization context. */
    private static final List<String> EXEMPT_PREFIXES = List.of(
            // /api/kernel = reverse-proxy Kernel : le backend ne consomme pas le contexte org,
            // il relaie tel quel l'éventuel X-Organization-Id au Kernel qui gère sa propre logique.
            "/api/kernel",
            "/api/auth", "/.well-known", "/actuator", "/swagger", "/v3/api-docs", "/webjars", "/favicon");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        UUID tenantId = parseUuid(firstNonBlank(
                request.getHeaders().getFirst(tenantHeaderName),
                request.getQueryParams().getFirst("tenantId")));

        String orgRaw = firstNonBlank(
                request.getHeaders().getFirst(orgHeaderName),
                request.getQueryParams().getFirst("organizationId"));
        UUID organizationId = parseUuid(orgRaw);

        // No explicit organization resolved.
        if (organizationId == null) {
            // CORS preflight (OPTIONS) never carries custom headers, and infrastructure paths
            // (auth, health, docs…) don't need an organization — let them through.
            if (org.springframework.http.HttpMethod.OPTIONS.equals(request.getMethod())
                    || isExempt(request)) {
                return writeContext(chain.filter(exchange), null, tenantId);
            }
            if (requireExplicit) {
                log.warn("Rejected {} {}: no organization context ({} header / organizationId param) "
                                + "and app.organization.require-explicit=true",
                        request.getMethod(), request.getPath(), orgHeaderName);
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
            log.warn("No organization context on {} {} — FALLING BACK to default organization {}. "
                            + "Set app.organization.require-explicit=true to fail-closed instead.",
                    request.getMethod(), request.getPath(), defaultOrganizationId);
            organizationId = parseUuid(defaultOrganizationId);
        }

        log.debug("Context resolved: organizationId={} tenantId={}", organizationId, tenantId);
        return writeContext(chain.filter(exchange), organizationId, tenantId);
    }

    private Mono<Void> writeContext(Mono<Void> downstream, UUID organizationId, UUID tenantId) {
        return downstream.contextWrite(ctx -> {
            Context c = ctx;
            if (organizationId != null) {
                c = c.put(ReactiveOrganizationContext.ORGANIZATION_ID_KEY, organizationId);
            }
            if (tenantId != null) {
                c = c.put(ReactiveOrganizationContext.TENANT_ID_KEY, tenantId);
            }
            return c;
        });
    }

    private boolean isExempt(ServerHttpRequest request) {
        String path = request.getPath().value();
        // Derrière Traefik (stripprefix /accounting-api + SERVER_FORWARD_HEADERS_STRATEGY=framework),
        // le chemin vu ici réintègre le préfixe via X-Forwarded-Prefix. On le retire avant de tester
        // l'exemption, sinon /accounting-api/actuator/health ne matcherait plus /actuator.
        String forwardedPrefix = request.getHeaders().getFirst("X-Forwarded-Prefix");
        if (forwardedPrefix != null && !forwardedPrefix.isBlank() && path.startsWith(forwardedPrefix)) {
            path = path.substring(forwardedPrefix.length());
            if (path.isEmpty()) {
                path = "/";
            }
        }
        final String effectivePath = path;
        return EXEMPT_PREFIXES.stream().anyMatch(effectivePath::startsWith);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for tenant/organization header: {}", value);
            return null;
        }
    }
}
