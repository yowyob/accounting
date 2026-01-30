package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive Service for generating accounting reports (balance sheet, income
 * statement).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportService {

        private final CompteRepository compte_repository;
        private final DetailEcritureRepository detail_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;
        private final JournalAuditRepository audit_repository;

        private static final String CACHE_BILAN = "rapport:bilan:";
        private static final String CACHE_RESULTAT = "rapport:resultat:";

        /**
         * Generates a balance sheet (assets/liabilities) for a given tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<Map<String, Object>> generateBilan(UUID tenant_id, String date_debut, String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);
                String cache_key = CACHE_BILAN + tenant_id + ":" + start + ":" + end;

                return redis_service.get(cache_key, Map.class)
                                .map(map -> (Map<String, Object>) map)
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("📊 Generating balance sheet from {} to {} for tenant {}", start, end,
                                                        tenant_id);

                                        return detail_repository
                                                        .findByTenant_IdAndDateRange(tenant_id, start.atStartOfDay(),
                                                                        end.plusDays(1).atStartOfDay())
                                                        .collectList()
                                                        .flatMap(allDetails -> compte_repository
                                                                        .findAllByTenant_Id(tenant_id)
                                                                        .flatMap(compte -> {
                                                                                BigDecimal solde = calculateAccountBalance(
                                                                                                allDetails, compte);
                                                                                compte.setSolde(solde);
                                                                                return compte_repository.save(compte)
                                                                                                .thenReturn(compte);
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(comptes -> {
                                                                                BigDecimal total_actif = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && (c.getClasse() == 1
                                                                                                                                || c.getClasse() == 2
                                                                                                                                || c.getClasse() == 5))
                                                                                                .map(Compte::getSolde)
                                                                                                .reduce(BigDecimal.ZERO,
                                                                                                                BigDecimal::add);

                                                                                BigDecimal total_passif = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && (c.getClasse() == 3
                                                                                                                                || c.getClasse() == 4))
                                                                                                .map(Compte::getSolde)
                                                                                                .reduce(BigDecimal.ZERO,
                                                                                                                BigDecimal::add);

                                                                                Map<String, Object> bilan = new HashMap<>();
                                                                                bilan.put("totalActif", total_actif);
                                                                                bilan.put("totalPassif", total_passif);
                                                                                bilan.put("equilibre", total_actif
                                                                                                .subtract(total_passif));

                                                                                return redis_service.save(cache_key,
                                                                                                bilan,
                                                                                                Duration.ofMinutes(30))
                                                                                                .then(ReactiveTenantContext
                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                .flatMap(tenant -> ReactiveTenantContext
                                                                                                                                .getCurrentUser()
                                                                                                                                .defaultIfEmpty("system")
                                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                                tenant,
                                                                                                                                                user,
                                                                                                                                                "BILAN_GENERATED",
                                                                                                                                                "Balance sheet generated from "
                                                                                                                                                                + date_debut
                                                                                                                                                                + " to "
                                                                                                                                                                + date_fin))))
                                                                                                .thenReturn(bilan);
                                                                        }));
                                }));
        }

        /**
         * Generates an income statement (expenses/revenues) for a given tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<Map<String, Object>> generateCompteResultat(UUID tenant_id, String date_debut, String date_fin) {
                LocalDate start = LocalDate.parse(date_debut);
                LocalDate end = LocalDate.parse(date_fin);
                String cache_key = CACHE_RESULTAT + tenant_id + ":" + start + ":" + end;

                return redis_service.get(cache_key, Map.class)
                                .map(map -> (Map<String, Object>) map)
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("📘 Generating income statement from {} to {} for tenant {}", start,
                                                        end, tenant_id);

                                        return detail_repository
                                                        .findByTenant_IdAndDateRange(tenant_id, start.atStartOfDay(),
                                                                        end.plusDays(1).atStartOfDay())
                                                        .collectList()
                                                        .flatMap(allDetails -> compte_repository
                                                                        .findAllByTenant_Id(tenant_id)
                                                                        .flatMap(compte -> {
                                                                                BigDecimal solde = calculateAccountBalance(
                                                                                                allDetails, compte);
                                                                                compte.setSolde(solde);
                                                                                return compte_repository.save(compte)
                                                                                                .thenReturn(compte);
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(comptes -> {
                                                                                BigDecimal total_charges = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 6)
                                                                                                .map(c -> c.getSolde()
                                                                                                                .abs()
                                                                                                                .negate())
                                                                                                .reduce(BigDecimal.ZERO,
                                                                                                                BigDecimal::add);

                                                                                BigDecimal total_produits = comptes
                                                                                                .stream()
                                                                                                .filter(c -> c.getClasse() != null
                                                                                                                && c.getClasse() == 7)
                                                                                                .map(c -> c.getSolde()
                                                                                                                .abs())
                                                                                                .reduce(BigDecimal.ZERO,
                                                                                                                BigDecimal::add);

                                                                                BigDecimal resultat = total_produits
                                                                                                .add(total_charges
                                                                                                                .negate());

                                                                                Map<String, Object> result_map = new HashMap<>();
                                                                                result_map.put("totalCharges",
                                                                                                total_charges.abs());
                                                                                result_map.put("totalProduits",
                                                                                                total_produits);
                                                                                result_map.put("resultatNet", resultat);

                                                                                return redis_service.save(cache_key,
                                                                                                result_map,
                                                                                                Duration.ofMinutes(30))
                                                                                                .then(ReactiveTenantContext
                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                .flatMap(tenant -> ReactiveTenantContext
                                                                                                                                .getCurrentUser()
                                                                                                                                .defaultIfEmpty("system")
                                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                                tenant,
                                                                                                                                                user,
                                                                                                                                                "COMPTE_RESULTAT_GENERATED",
                                                                                                                                                "Income statement generated from "
                                                                                                                                                                + date_debut
                                                                                                                                                                + " to "
                                                                                                                                                                + date_fin))))
                                                                                                .thenReturn(result_map);
                                                                        }));
                                }));
        }

        private BigDecimal calculateAccountBalance(List<DetailEcriture> details, Compte compte) {
                return details.stream()
                                .filter(d -> d.getCompte_id() != null && d.getCompte_id().equals(compte.getId()))
                                .map(d -> {
                                        BigDecimal debit = d.getMontant_debit() != null ? d.getMontant_debit()
                                                        : BigDecimal.ZERO;
                                        BigDecimal credit = d.getMontant_credit() != null ? d.getMontant_credit()
                                                        : BigDecimal.ZERO;
                                        return "ACTIF".equals(compte.getType_compte())
                                                        || "CHARGE".equals(compte.getType_compte())
                                                                        ? debit.subtract(credit)
                                                                        : credit.subtract(debit);
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private Mono<Void> logAudit(Tenant tenant, String utilisateur, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant.getId())
                                .action(action)
                                .utilisateur(utilisateur)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by("system")
                                .updated_by("system")
                                .build();

                return audit_repository.save(audit)
                                .flatMap(saved -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
                                        return Mono.empty();
                                });
        }
}