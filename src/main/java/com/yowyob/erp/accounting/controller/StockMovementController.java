package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.StockMovementService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for stock movements and their accounting impact.
 * 
 * @author ALD
 * @date 20.01.26
 */
@RestController
@RequestMapping("/api/comptable/stock")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mouvements de Stock", description = "Endpoints pour les mouvements de stock et leur impact comptable")
@SecurityRequirement(name = "Bearer Authentication")
public class StockMovementController {

        private final StockMovementService stock_service;

        /**
         * Creates a stock movement (entry, exit, transfer).
         * 
         * @param mouvement the movement data
         * @return created movement with accounting impact
         */
        @PostMapping("/mouvement")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Créer un mouvement de stock", description = "Enregistre un mouvement de stock (entrée, sortie, transfert) et génère l'impact comptable")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Mouvement créé avec succès"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
        })
        public reactor.core.publisher.Mono<ResponseEntity<ApiResponseWrapper<Map<String, Object>>>> creerMouvement(
                        @RequestBody Map<String, Object> mouvement) {

                return com.yowyob.erp.config.organization.ReactiveOrganizationContext.getCurrentUser()
                                .defaultIfEmpty("system")
                                .flatMap(user -> stock_service.creerMouvementStock(mouvement, user)
                                                .map(resultat -> ResponseEntity.status(HttpStatus.CREATED)
                                                                .body(ApiResponseWrapper.success(
                                                                                resultat,
                                                                                "Mouvement de stock créé avec succès")))
                                                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal()));
        }

        /**
         * Gets all stock movements for the current tenant.
         * 
         * @return list of stock movements
         */
        @GetMapping("/mouvements")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Liste des mouvements de stock", description = "Retourne tous les mouvements de stock du tenant")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste récupérée"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
        })
        public reactor.core.publisher.Mono<ResponseEntity<ApiResponseWrapper<List<Map<String, Object>>>>> getMouvements(
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String produit_id) {

                return com.yowyob.erp.config.organization.ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        log.info("📋 Getting stock movements for tenant {}", organization_id);
                                        return stock_service.getMouvements(organization_id, type, produit_id)
                                                        .collectList()
                                                        .map(mouvements -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                                        mouvements,
                                                                        "Liste des mouvements récupérée")))
                                                        .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
                                });
        }

        /**
         * Gets the accounting impact of a stock movement.
         * 
         * @param mouvementId the movement ID
         * @return accounting entries generated by this movement
         */
        @GetMapping("/impact-comptable/{mouvementId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Impact comptable d'un mouvement", description = "Retourne les écritures comptables générées par un mouvement de stock")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Impact récupéré"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mouvement non trouvé")
        })
        public reactor.core.publisher.Mono<ResponseEntity<ApiResponseWrapper<Map<String, Object>>>> getImpactComptable(
                        @PathVariable UUID mouvementId) {
                return com.yowyob.erp.config.organization.ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        log.info("💰 Getting accounting impact for stock movement {} of tenant {}",
                                                        mouvementId, organization_id);
                                        return stock_service.getImpactComptable(mouvementId)
                                                        .map(impact -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                                        impact,
                                                                        "Impact comptable récupéré")))
                                                        .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
                                });
        }
}
