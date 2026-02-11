// Utilitaires pour la validation
package com.yowyob.erp.common.util;

import com.yowyob.erp.common.exception.TenantException;
import com.yowyob.erp.config.organization.OrganizationContext;
import java.util.UUID;

public class ValidationUtils {

    public static void validateTenantAccess(UUID resourceTenantId) {
        UUID currentOrganization = OrganizationContext.getCurrentTenant();
        if (currentOrganization == null) {
            throw new TenantException("Contexte tenant non défini");
        }
        if (!currentOrganization.equals(resourceTenantId)) {
            throw new TenantException("Accès refusé à la ressource d'un autre tenant");
        }
    }

    public static boolean isValidAccountNumber(String accountNumber) {
        // Validation selon le plan comptable OHADA
        return accountNumber != null && 
               accountNumber.matches("^[1-8][0-9]{4,7}$");
    }

    public static boolean isValidAmount(Double amount) {
        return amount != null && amount >= 0;
    }

    public static String generateTransactionReference(String prefix) {
        return String.format("%s-%d-%s", 
                prefix, 
                System.currentTimeMillis(),
                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}