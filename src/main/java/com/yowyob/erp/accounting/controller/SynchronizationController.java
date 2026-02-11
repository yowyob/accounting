package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.SynchronizationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for manual synchronization operations (Elasticsearch, Redis).
 * 
 * @author ALD
 * @date 20.01.26
 */
@RestController
@RequestMapping("/api/comptable/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Synchronisation", description = "Endpoints pour la synchronisation manuelle Elasticsearch et Redis")
@SecurityRequirement(name = "Bearer Authentication")
public class SynchronizationController {

        private final SynchronizationService sync_service;

        /**
         * Forces synchronization of all accounting entries to Elasticsearch.
         * 
         * @return synchronization result with indexed entries count
         */
        @PostMapping("/elasticsearch")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Forcer la synchronisation Elasticsearch", description = "Réindexe toutes les écritures comptables dans Elasticsearch (Admin uniquement)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Synchronisation effectuée"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé - Admin uniquement")
        })
        public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> syncElasticsearch() {
                UUID organization_id = OrganizationContext.getCurrentTenant();
                log.info("🔄 Forcing Elasticsearch synchronization for tenant {}", organization_id);

                Map<String, Object> resultat = sync_service.synchroniserElasticsearch(organization_id);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                resultat,
                                "Synchronisation Elasticsearch effectuée"));
        }

        /**
         * Clears all Redis cache for the current tenant.
         * 
         * @return cache clearing result
         */
        @PostMapping("/redis/clear")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Vider le cache Redis", description = "Supprime tous les caches Redis du tenant (Admin uniquement)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cache vidé"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé - Admin uniquement")
        })
        public ResponseEntity<ApiResponseWrapper<String>> clearRedisCache() {
                UUID organization_id = OrganizationContext.getCurrentTenant();
                log.warn("🗑️ Clearing Redis cache for tenant {}", organization_id);

                sync_service.viderCacheRedis(organization_id);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                "Cache vidé",
                                "Tous les caches Redis du tenant ont été supprimés"));
        }

        /**
         * Gets the synchronization status for the current tenant.
         * 
         * @return synchronization statistics
         */
        @GetMapping("/status")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Statut de la synchronisation", description = "Retourne les statistiques de synchronisation Elasticsearch et Redis")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut récupéré"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
        })
        public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getSyncStatus() {
                UUID organization_id = OrganizationContext.getCurrentTenant();
                log.info("📊 Getting synchronization status for tenant {}", organization_id);

                Map<String, Object> statut = sync_service.getStatutSynchronisation(organization_id);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                statut,
                                "Statut de synchronisation récupéré"));
        }
}
