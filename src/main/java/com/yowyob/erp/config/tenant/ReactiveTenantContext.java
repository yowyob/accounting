package com.yowyob.erp.config.tenant;

import com.yowyob.erp.accounting.entity.Tenant;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Reactive tenant context to isolate data per tenant in WebFlux.
 * Uses Reactor Context instead of ThreadLocal.
 */
public class ReactiveTenantContext {

    public static final String TENANT_ID_KEY = "tenantId";
    public static final String USER_ID_KEY = "userId";

    /**
     * Gets the tenant ID from the Reactor Context.
     */
    public static Mono<UUID> getTenantId() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(TENANT_ID_KEY)) {
                return Mono.just(ctx.get(TENANT_ID_KEY));
            }
            // Fallback for hybrid Servlet/Reactive environments
            UUID threadLocalTenantId = TenantContext.getCurrentTenant();
            if (threadLocalTenantId != null) {
                return Mono.just(threadLocalTenantId);
            }
            return Mono.empty();
        });
    }

    /**
     * Gets the current tenant as a Tenant entity instance.
     */
    public static Mono<Tenant> getCurrentTenantAsTenant() {
        return getTenantId().map(Tenant::new);
    }

    /**
     * Gets the current user from the Reactor Context.
     */
    public static Mono<String> getCurrentUser() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(USER_ID_KEY)) {
                return Mono.just(ctx.get(USER_ID_KEY));
            }
            return Mono.just("system");
        });
    }

    /**
     * Creates a Reactor Context with the given tenant ID.
     */
    public static Context withTenantId(UUID tenantId) {
        return Context.of(TENANT_ID_KEY, tenantId);
    }

    /**
     * Creates a Reactor Context with the given tenant ID and user.
     */
    public static Context withTenantAndUser(UUID tenantId, String user) {
        return Context.of(TENANT_ID_KEY, tenantId, USER_ID_KEY, user);
    }

    /**
     * Captures the current tenant and user from the ThreadLocal context (TenantContext)
     * and returns a Reactor Context.
     */
    public static Context captureFromThreadLocal() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = TenantContext.getCurrentUser();
        
        Context ctx = Context.empty();
        if (tenantId != null) {
            ctx = ctx.put(TENANT_ID_KEY, tenantId);
        }
        if (user != null) {
            ctx = ctx.put(USER_ID_KEY, user);
        }
        return ctx;
    }
}
