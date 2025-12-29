package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating accounting reports (balance sheet, income statement).
 * Uses Redis for caching and Kafka for traceability.
 *
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportService {

    private final CompteRepository compte_repository;
    private final DetailEcritureRepository detail_repository;
    private final RedisService redis_service;
    private final KafkaMessageService kafka_service;

    private static final String CACHE_BILAN = "rapport:bilan:";
    private static final String CACHE_RESULTAT = "rapport:resultat:";

    /**
     * Generates a balance sheet (assets/liabilities) for a given tenant.
     * 
     * @param tenant_id  the tenant ID
     * @param date_debut period start date (ISO string)
     * @param date_fin   period end date (ISO string)
     * @return map containing assets, liabilities, and balance offset
     */
    public Map<String, Object> generateBilan(UUID tenant_id, String date_debut, String date_fin) {
        LocalDate start = LocalDate.parse(date_debut);
        LocalDate end = LocalDate.parse(date_fin);
        String cache_key = CACHE_BILAN + tenant_id + ":" + start + ":" + end;

        // Check Redis
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = redis_service.get(cache_key, Map.class);
        if (cached != null) {
            log.info("♻️ Balance sheet retrieved from Redis cache for tenant {}", tenant_id);
            return cached;
        }

        log.info("📊 Generating balance sheet from {} to {} for tenant {}", start, end, tenant_id);
        List<Compte> comptes = compte_repository.findAllByTenant_Id(tenant_id);

        BigDecimal total_actif = BigDecimal.ZERO;
        BigDecimal total_passif = BigDecimal.ZERO;

        for (Compte compte : comptes) {
            // Fetch all related entries for the date range
            List<DetailEcriture> details = detail_repository.findByTenant_IdAndDateRange(
                    tenant_id,
                    start.atStartOfDay(),
                    end.plusDays(1).atStartOfDay());

            // Calculate account balance based on account type
            BigDecimal solde = calculateAccountBalance(details, compte);
            compte.updateSolde(solde); // Update the account balance
            compte_repository.save(compte); // Persist the updated balance

            // Aggregate based on OHADA class
            if (compte.getClasse() != null
                    && (compte.getClasse() == 1 || compte.getClasse() == 2 || compte.getClasse() == 5)) {
                total_actif = total_actif.add(solde);
            } else if (compte.getClasse() != null && (compte.getClasse() == 3 || compte.getClasse() == 4)) {
                total_passif = total_passif.add(solde);
            }
        }

        Map<String, Object> bilan = new HashMap<>();
        bilan.put("totalActif", total_actif);
        bilan.put("totalPassif", total_passif);
        bilan.put("equilibre", total_actif.subtract(total_passif));

        // Save to Redis
        redis_service.save(cache_key, bilan, java.time.Duration.ofMinutes(30));

        // Publish Kafka event
        kafka_service.sendAuditLog(bilan, tenant_id.toString(), "BILAN_GENERATED");

        log.info("✅ Balance sheet generated: Actif={} | Passif={} | Tenant={}", total_actif, total_passif, tenant_id);
        return bilan;
    }

    /**
     * Generates an income statement (expenses/revenues) for a given tenant.
     * 
     * @param tenant_id  the tenant ID
     * @param date_debut period start date (ISO string)
     * @param date_fin   period end date (ISO string)
     * @return map containing total expenses, total products, and net result
     */
    public Map<String, Object> generateCompteResultat(UUID tenant_id, String date_debut, String date_fin) {
        LocalDate start = LocalDate.parse(date_debut);
        LocalDate end = LocalDate.parse(date_fin);
        String cache_key = CACHE_RESULTAT + tenant_id + ":" + start + ":" + end;

        // Check Redis
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = redis_service.get(cache_key, Map.class);
        if (cached != null) {
            log.info("♻️ Income statement retrieved from Redis cache for tenant {}", tenant_id);
            return cached;
        }

        log.info("📘 Generating income statement from {} to {} for tenant {}", start, end, tenant_id);
        List<Compte> comptes = compte_repository.findAllByTenant_Id(tenant_id);

        BigDecimal total_charges = BigDecimal.ZERO;
        BigDecimal total_produits = BigDecimal.ZERO;

        for (Compte compte : comptes) {
            List<DetailEcriture> details = detail_repository.findByTenant_IdAndDateRange(
                    tenant_id,
                    start.atStartOfDay(),
                    end.plusDays(1).atStartOfDay());

            BigDecimal solde = calculateAccountBalance(details, compte);
            compte.updateSolde(solde); // Update the account balance
            compte_repository.save(compte); // Persist the updated balance

            if (compte.getClasse() != null && compte.getClasse() == 6) {
                total_charges = total_charges.add(solde.abs().negate()); // Charges are debits, so negative
            } else if (compte.getClasse() != null && compte.getClasse() == 7) {
                total_produits = total_produits.add(solde.abs()); // Products are credits, so positive
            }
        }

        BigDecimal resultat = total_produits.add(total_charges.negate()); // Result = Products - Charges

        Map<String, Object> compte_resultat = new HashMap<>();
        compte_resultat.put("totalCharges", total_charges.abs());
        compte_resultat.put("totalProduits", total_produits);
        compte_resultat.put("resultatNet", resultat);

        // Save to Redis
        redis_service.save(cache_key, compte_resultat, java.time.Duration.ofMinutes(30));

        // Publish Kafka event
        kafka_service.sendAuditLog(compte_resultat, tenant_id.toString(), "COMPTE_RESULTAT_GENERATED");

        log.info("✅ Income statement generated: Charges={} | Produits={} | Resultat={} | Tenant={}",
                total_charges.abs(), total_produits, resultat, tenant_id);
        return compte_resultat;
    }

    /**
     * Calculates the balance of an account based on its type and related entries.
     * 
     * @param details list of entry details to process
     * @param compte  the account being balanced
     * @return balanced amount
     */
    private BigDecimal calculateAccountBalance(List<DetailEcriture> details, Compte compte) {
        return details.stream()
                .filter(d -> d.getCompte() != null && d.getCompte().getId() != null
                        && d.getCompte().getId().equals(compte.getId()))
                .map(d -> {
                    BigDecimal debit = d.getMontant_debit();
                    BigDecimal credit = d.getMontant_credit();
                    return "ACTIF".equals(compte.getType_compte()) || "CHARGE".equals(compte.getType_compte())
                            ? debit.subtract(credit)
                            : credit.subtract(debit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}