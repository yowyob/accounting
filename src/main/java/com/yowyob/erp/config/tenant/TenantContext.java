package com.yowyob.erp.config.tenant;

import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.TenantRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant context to isolate data per tenant.
 * Uses ThreadLocal to maintain the tenantId per thread.
 *
 * @author ALD
 * @date 12/10/2025 06:19 AM WAT
 */
@Component
public class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static TenantRepository tenantRepository; // Initialized statically

    @Autowired
    public TenantContext(TenantRepository tenantRepository) {
        TenantContext.tenantRepository = tenantRepository; // Set the static reference
    }

    /**
     * Sets the current tenant ID for the current thread.
     * @param tenantId the UUID of the tenant
     * @throws IllegalArgumentException if tenantId is null
     */
    public static void setCurrentTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        currentTenant.set(tenantId);
    }

    /**
     * Gets the current tenant ID for the current thread.
     * @return the tenant ID or null if not set
     */
    public static UUID getCurrentTenant() {
        return currentTenant.get();
    }

    /**
     * Gets the current tenant object for the current thread.
     * @return the Tenant object
     * @throws ResourceNotFoundException if the tenant ID is not found in the repository or if no tenant context is set
     */
    public static Tenant getCurrentTenantAsTenant() {
        UUID tenantId = getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context is set");
        }
        if (tenantRepository == null) {
            throw new IllegalStateException("Tenant repository is not initialized");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));
    }

    /**
     * Clears the tenant context for the current thread.
     */
    public static void clear() {
        currentTenant.remove();
        currentUser.remove();
    }

    /**
     * Sets the current user for the current thread.
     * @param user the username (max 255 characters)
     */
    public static void setCurrentUser(@Size(max = 255, message = "User name must not exceed 255 characters") String user) {
        currentUser.set(user);
    }

    /**
     * Gets the current user for the current thread.
     * @return the username or "system" if not set
     */
    public static String getCurrentUser() {
        return Optional.ofNullable(currentUser.get()).orElse("system");
    }
}