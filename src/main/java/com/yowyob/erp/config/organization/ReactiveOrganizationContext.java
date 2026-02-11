package com.yowyob.erp.config.organization;

import com.yowyob.erp.accounting.entity.Organization;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Reactive organization context to isolate data per organization in WebFlux.
 * Uses Reactor Context instead of ThreadLocal.
 */
public class ReactiveOrganizationContext {

    public static final String ORGANIZATION_ID_KEY = "organizationId";
    public static final String USER_ID_KEY = "userId";

    /**
     * Gets the organization ID from the Reactor Context.
     */
    public static Mono<UUID> getOrganizationId() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(ORGANIZATION_ID_KEY)) {
                return Mono.just(ctx.get(ORGANIZATION_ID_KEY));
            }
            // Fallback for hybrid Servlet/Reactive environments
            UUID threadLocalOrganizationId = OrganizationContext.getCurrentOrganization();
            if (threadLocalOrganizationId != null) {
                return Mono.just(threadLocalOrganizationId);
            }
            System.err.println("CRITICAL: Organization ID not found in Context or ThreadLocal!");
            return Mono.empty();
        });
    }

    /**
     * Gets the current organization as an Organization entity instance.
     */
    public static Mono<Organization> getCurrentOrganization() {
        return getOrganizationId().map(Organization::new);
    }

    public static Mono<Organization> getCurrentOrganizationAsOrganization() {
        return getCurrentOrganization();
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
     * Creates a Reactor Context with the given organization ID.
     */
    public static Context withOrganizationId(UUID organizationId) {
        return Context.of(ORGANIZATION_ID_KEY, organizationId);
    }

    /**
     * Creates a Reactor Context with the given organization ID and user.
     */
    public static Context withOrganizationAndUser(UUID organizationId, String user) {
        return Context.of(ORGANIZATION_ID_KEY, organizationId, USER_ID_KEY, user);
    }

    /**
     * Captures the current organization and user from the ThreadLocal context
     * (OrganizationContext)
     * and returns a Reactor Context.
     */
    public static Context captureFromThreadLocal() {
        UUID organizationId = OrganizationContext.getCurrentOrganization();
        String user = OrganizationContext.getCurrentUser();

        Context ctx = Context.empty();
        if (organizationId != null) {
            ctx = ctx.put(ORGANIZATION_ID_KEY, organizationId);
        }
        if (user != null) {
            ctx = ctx.put(USER_ID_KEY, user);
        }
        return ctx;
    }
}
