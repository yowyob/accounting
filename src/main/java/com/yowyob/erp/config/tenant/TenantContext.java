package com.yowyob.erp.config.tenant;

import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.TenantRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant context to isolate data per tenant.
 * Note: For reactive flows, use ReactiveTenantContext where possible.
 */
@Component
public class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static TenantRepository tenantRepository;

    @Autowired
    public TenantContext(TenantRepository tenantRepository) {
        TenantContext.tenantRepository = tenantRepository;
    }

    public static void setCurrentTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        currentTenant.set(tenantId);
    }

    public static UUID getCurrentTenant() {
        return currentTenant.get();
    }

    /**
     * Gets the current tenant object as a Mono.
     */
    public static Mono<Tenant> getCurrentTenantAsTenant() {
        UUID tenantId = getCurrentTenant();
        if (tenantId == null) {
            return Mono.error(new IllegalStateException("No tenant context is set"));
        }
        if (tenantRepository == null) {
            return Mono.error(new IllegalStateException("Tenant repository is not initialized"));
        }
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Tenant", tenantId.toString())));
    }

    public static void clear() {
        currentTenant.remove();
        currentUser.remove();
    }

    public static void setCurrentUser(
            @Size(max = 255, message = "User name must not exceed 255 characters") String user) {
        currentUser.set(user);
    }

    public static String getCurrentUser() {
        return Optional.ofNullable(currentUser.get()).orElse("system");
    }
}