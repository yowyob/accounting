package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating accounting reports (balance sheet, income statement).
 * Uses Redis for caching and Kafka for traceability.
 *
 * @author ALD
 * @date 12/10/2025 03:59 PM WAT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportService {

    private final CompteRepository compteRepository;
    private final DetailEcritureRepository detailEcritureRepository;
    private final RedisService redisService;
    private final KafkaMessageService kafkaMessageService;

    private static final String CACHE_BILAN = "rapport:bilan:";
    private static final String CACHE_RESULTAT = "rapport:resultat:";

    /**
     * Generates a balance sheet (assets/liabilities) for a given tenant.
     */
    public Map<String, Object> generateBilan(UUID tenantId, String dateDebut, String dateFin) {
        LocalDate start = LocalDate.parse(dateDebut);
        LocalDate end = LocalDate.parse(dateFin);
        String cacheKey = CACHE_BILAN + tenantId + ":" + start + ":" + end;

        // Check Redis
        Map<String, Object> cached = redisService.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("♻️ Balance sheet retrieved from Redis cache for tenant {}", tenantId);
            return cached;
        }

        log.info("📊 Generating balance sheet from {} to {} for tenant {}", start, end, tenantId);
        List<Compte> comptes = compteRepository.findAllByTenant_Id(tenantId);

        BigDecimal totalActif = BigDecimal.ZERO;
        BigDecimal totalPassif = BigDecimal.ZERO;

        for (Compte compte : comptes) {
            // Fetch all related entries for the date range
            List<DetailEcriture> details = detailEcritureRepository.findByTenant_IdAndDateRange(
                    tenantId,
                    start.atStartOfDay(),
                    end.plusDays(1).atStartOfDay()
            );

            // Calculate account balance based on account type
            BigDecimal solde = calculateAccountBalance(details, compte);
            compte.updateSolde(solde); // Update the account balance
            compteRepository.save(compte); // Persist the updated balance

            // Aggregate based on OHADA class
            if (compte.getClasse() != null && (compte.getClasse() == 1 || compte.getClasse() == 2 || compte.getClasse() == 5)) {
                totalActif = totalActif.add(solde);
            } else if (compte.getClasse() != null && (compte.getClasse() == 3 || compte.getClasse() == 4)) {
                totalPassif = totalPassif.add(solde);
            }
        }

        Map<String, Object> bilan = new HashMap<>();
        bilan.put("totalActif", totalActif);
        bilan.put("totalPassif", totalPassif);
        bilan.put("equilibre", totalActif.subtract(totalPassif));

        // Save to Redis
        redisService.save(cacheKey, bilan, java.time.Duration.ofMinutes(30));

        // Publish Kafka event
        kafkaMessageService.sendAuditLog(bilan, tenantId.toString(), "BILAN_GENERATED");

        log.info("✅ Balance sheet generated: Actif={} | Passif={} | Tenant={}", totalActif, totalPassif, tenantId);
        return bilan;
    }

    /**
     * Generates an income statement (expenses/revenues) for a given tenant.
     */
    public Map<String, Object> generateCompteResultat(UUID tenantId, String dateDebut, String dateFin) {
        LocalDate start = LocalDate.parse(dateDebut);
        LocalDate end = LocalDate.parse(dateFin);
        String cacheKey = CACHE_RESULTAT + tenantId + ":" + start + ":" + end;

        // Check Redis
        Map<String, Object> cached = redisService.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("♻️ Income statement retrieved from Redis cache for tenant {}", tenantId);
            return cached;
        }

        log.info("📘 Generating income statement from {} to {} for tenant {}", start, end, tenantId);
        List<Compte> comptes = compteRepository.findAllByTenant_Id(tenantId);

        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalProduits = BigDecimal.ZERO;

        for (Compte compte : comptes) {
            List<DetailEcriture> details = detailEcritureRepository.findByTenant_IdAndDateRange(
                    tenantId,
                    start.atStartOfDay(),
                    end.plusDays(1).atStartOfDay()
            );

            BigDecimal solde = calculateAccountBalance(details, compte);
            compte.updateSolde(solde); // Update the account balance
            compteRepository.save(compte); // Persist the updated balance

            if (compte.getClasse() != null && compte.getClasse() == 6) {
                totalCharges = totalCharges.add(solde.abs().negate()); // Charges are debits, so negative
            } else if (compte.getClasse() != null && compte.getClasse() == 7) {
                totalProduits = totalProduits.add(solde.abs()); // Products are credits, so positive
            }
        }

        BigDecimal resultat = totalProduits.add(totalCharges.negate()); // Result = Products - Charges

        Map<String, Object> compteResultat = new HashMap<>();
        compteResultat.put("totalCharges", totalCharges.abs());
        compteResultat.put("totalProduits", totalProduits);
        compteResultat.put("resultatNet", resultat);

        // Save to Redis
        redisService.save(cacheKey, compteResultat, java.time.Duration.ofMinutes(30));

        // Publish Kafka event
        kafkaMessageService.sendAuditLog(compteResultat, tenantId.toString(), "COMPTE_RESULTAT_GENERATED");

        log.info("✅ Income statement generated: Charges={} | Produits={} | Resultat={} | Tenant={}",
                totalCharges.abs(), totalProduits, resultat, tenantId);
        return compteResultat;
    }

    /**
     * Calculates the balance of an account based on its type and related entries.
     */
    private BigDecimal calculateAccountBalance(List<DetailEcriture> details, Compte compte) {
        return details.stream()
                .filter(d -> d.getCompte() != null && d.getCompte().getId() != null && d.getCompte().getId().equals(compte.getId()))
                .map(d -> {
                    BigDecimal debit = d.getMontantDebit();
                    BigDecimal credit = d.getMontantCredit();
                    return "ACTIF".equals(compte.getTypeCompte()) || "CHARGE".equals(compte.getTypeCompte()) ?
                            debit.subtract(credit) : credit.subtract(debit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}