package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.LettrageAutomatiqueService;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.yowyob.erp.config.auth.AccountingAuthorities;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
         * Launches automatic reconciliation for the current organization.
         * Matches debit and credit entries with the same account and amount.
         * 
         * @return number of reconciled entry pairs
         */
        @PostMapping("/auto")
        @PreAuthorize(AccountingAuthorities.MANAGE)
        @Operation(summary = "Lancer le lettrage automatique", description = "Rapproche automatiquement les écritures débit/crédit avec le même compte et montant")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lettrage effectué avec succès"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
        })
        public reactor.core.publisher.Mono<ResponseEntity<ApiResponseWrapper<Integer>>> lancerLettrageAutomatique() {
                UUID organization_id = OrganizationContext.getCurrentOrganization();
                log.info("🔗 Launching automatic reconciliation for organization {}", organization_id);

                return lettrage_service.lettrerToutLeOrganization(organization_id)
                                .map(paires_lettrees -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                paires_lettrees,
                                                paires_lettrees + " paires d'écritures lettrées automatiquement")));
        }

        /**
         * Gets the reconciliation status for the current organization.
         * 
         * @return reconciliation statistics
         */
        @GetMapping("/status")
        @PreAuthorize(AccountingAuthorities.READ)
        @Operation(summary = "Obtenir le statut du lettrage", description = "Retourne les statistiques de lettrage pour le organization")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut récupéré"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
        })
        public ResponseEntity<ApiResponseWrapper<String>> getStatutLettrage() {
                UUID organization_id = OrganizationContext.getCurrentOrganization();
                log.info("📊 Getting reconciliation status for organization {}", organization_id);

                // This would require a new method in the service to get statistics
                String status = "Fonctionnalité de statistiques à implémenter dans LettrageAutomatiqueService";

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                status,
                                "Statut du lettrage récupéré"));
        }
}
