package com.yowyob.erp.config.organization;

import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Organization context to isolate data per organization.
 * Note: For reactive flows, use ReactiveOrganizationContext where possible.
 */
@Component
public class OrganizationContext {

    private static final ThreadLocal<UUID> currentOrganization = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static OrganizationRepository organizationRepository;

    @Autowired
    public OrganizationContext(OrganizationRepository organizationRepository) {
        OrganizationContext.organizationRepository = organizationRepository;
    }

    public static void setCurrentOrganization(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        currentOrganization.set(organizationId);
    }

    public static UUID getCurrentOrganizationId() {
        return currentOrganization.get();
    }

    public static UUID getCurrentOrganization() {
        return getCurrentOrganizationId();
    }

    /**
     * Gets the current organization object as a Mono.
     */
    public static Mono<Organization> getCurrentOrganizationAsOrganization() {
        UUID organizationId = getCurrentOrganizationId();
        if (organizationId == null) {
            return Mono.error(new IllegalStateException("No organization context is set"));
        }
        if (organizationRepository == null) {
            return Mono.error(new IllegalStateException("Organization repository is not initialized"));
        }
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Organization", organizationId.toString())));
    }

    public static void clear() {
        currentOrganization.remove();
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