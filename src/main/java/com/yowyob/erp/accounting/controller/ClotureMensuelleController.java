package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.ClotureMensuelleService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for monthly period closure operations.
 * 
 * @author ALD
 * @date 20.01.26
 */
@RestController
@RequestMapping("/api/comptable/cloture")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clôture Mensuelle", description = "Endpoints pour la clôture des périodes comptables")
@SecurityRequirement(name = "Bearer Authentication")
public class ClotureMensuelleController {

        private final ClotureMensuelleService cloture_service;

        /**
         * Closes a monthly accounting period.
         * Validates all entries and generates closing entries.
         * 
         * @param periodeId the period ID to close
         * @return closure result with generated entries
         */
        @PostMapping("/mensuelle/{periodeId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Clôturer une période mensuelle", description = "Valide toutes les écritures et génère les écritures de clôture")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Période clôturée avec succès"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Période non éligible à la clôture"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Période non trouvée")
        })
        public Mono<ResponseEntity<ApiResponseWrapper<Map<String, Object>>>> cloturerPeriode(
                        @PathVariable UUID periodeId) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .zipWith(ReactiveOrganizationContext.getCurrentUser())
                                .flatMap(tuple -> {
                                        UUID organization_id = tuple.getT1();
                                        String user = tuple.getT2();
                                        log.info("🔒 Closing period {} for tenant {} by user {}", periodeId, organization_id,
                                                        user);

                                        return cloture_service.cloturerPeriode(periodeId, user)
                                                        .map(resultat -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                                        resultat,
                                                                        "Période clôturée avec succès")))
                                                        .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
                                });
        }

        /**
         * Checks if a period is eligible for closure.
         * 
         * @param periodeId the period ID to check
         * @return eligibility status and validation details
         */
        @GetMapping("/status/{periodeId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Vérifier l'éligibilité à la clôture", description = "Vérifie si une période peut être clôturée (toutes les écritures validées, etc.)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut récupéré"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Période non trouvée")
        })
        public Mono<ResponseEntity<ApiResponseWrapper<Map<String, Object>>>> verifierEligibilite(
                        @PathVariable UUID periodeId) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        log.info("🔍 Checking closure eligibility for period {} of tenant {}",
                                                        periodeId, organization_id);
                                        return cloture_service.verifierEligibiliteCloture(periodeId)
                                                        .map(statut -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                                        statut,
                                                                        "Statut de clôture vérifié")))
                                                        .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
                                });
        }

        /**
         * Cancels a period closure (admin only).
         * 
         * @param periodeId the period ID to reopen
         * @return cancellation result
         */
        @PostMapping("/annuler/{periodeId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Annuler une clôture", description = "Réouvre une période clôturée (réservé aux administrateurs)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Clôture annulée"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé - Admin uniquement"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Période non trouvée")
        })
        public Mono<ResponseEntity<ApiResponseWrapper<String>>> annulerCloture(@PathVariable UUID periodeId) {
                return ReactiveOrganizationContext.getCurrentUser()
                                .flatMap(user -> {
                                        log.warn("⚠️ Cancelling closure for period {} by admin {}", periodeId, user);
                                        return cloture_service.annulerCloture(periodeId, user)
                                                        .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.success(
                                                                        "Clôture annulée",
                                                                        "La période a été réouverte avec succès"))))
                                                        .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
                                });
        }
}
