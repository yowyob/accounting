package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.LettrageAutomatiqueService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for automatic and manual reconciliation (lettrage) of accounting
 * entries.
 * 
 * @author ALD
 * @date 20.01.26
 */
@RestController
@RequestMapping("/api/comptable/lettrage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lettrage", description = "Endpoints pour le lettrage automatique et manuel des écritures")
@SecurityRequirement(name = "Bearer Authentication")
public class LettrageController {

        private final LettrageAutomatiqueService lettrage_service;

        /**
         * Launches automatic reconciliation for the current tenant.
         * Matches debit and credit entries with the same account and amount.
         * 
         * @return number of reconciled entry pairs
         */
        @PostMapping("/auto")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Lancer le lettrage automatique", description = "Rapproche automatiquement les écritures débit/crédit avec le même compte et montant")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lettrage effectué avec succès"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
        })
        public reactor.core.publisher.Mono<ResponseEntity<ApiResponseWrapper<Integer>>> lancerLettrageAutomatique() {
                UUID tenant_id = TenantContext.getCurrentTenant();
                log.info("🔗 Launching automatic reconciliation for tenant {}", tenant_id);

                return lettrage_service.lettrerToutLeTenant(tenant_id)
                                .map(paires_lettrees -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                paires_lettrees,
                                                paires_lettrees + " paires d'écritures lettrées automatiquement")));
        }

        /**
         * Gets the reconciliation status for the current tenant.
         * 
         * @return reconciliation statistics
         */
        @GetMapping("/status")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Obtenir le statut du lettrage", description = "Retourne les statistiques de lettrage pour le tenant")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut récupéré"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
        })
        public ResponseEntity<ApiResponseWrapper<String>> getStatutLettrage() {
                UUID tenant_id = TenantContext.getCurrentTenant();
                log.info("📊 Getting reconciliation status for tenant {}", tenant_id);

                // This would require a new method in the service to get statistics
                String status = "Fonctionnalité de statistiques à implémenter dans LettrageAutomatiqueService";

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                status,
                                "Statut du lettrage récupéré"));
        }
}
