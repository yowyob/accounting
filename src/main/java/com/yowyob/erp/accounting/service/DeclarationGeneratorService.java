package com.yowyob.erp.accounting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.Taxe;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.TaxeRepository;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.common.enums.Sens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 
 * Service to generate tax declarations by scanning accounting entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeclarationGeneratorService {

    private final DetailEcritureRepository detail_repository;
    private final TaxeRepository taxe_repository;
    private final CompteRepository compte_repository;
    private final ObjectMapper object_mapper;

    public Mono<DeclarationFiscale> generateVatDeclaration(UUID organization_id, LocalDate start_date, LocalDate end_date) {
        log.info("Generating VAT declaration for organization {} from {} to {}", organization_id, start_date, end_date);

        LocalDateTime start = start_date.atStartOfDay();
        LocalDateTime end = end_date.atTime(LocalTime.MAX);

        return taxe_repository.findByOrganization_IdAndActifTrue(organization_id)
                .collectList()
                .flatMap(taxes -> {
                    if (taxes.isEmpty()) {
                        return Mono.error(new RuntimeException("No active taxes found for this organization"));
                    }

                    Set<String> all_tax_accounts = new HashSet<>();
                    Map<String, String> account_to_type = new HashMap<>(); // account -> "COLLECTE" or "DEDUCTIBLE"

                    for (Taxe taxe : taxes) {
                        if (taxe.getCompte_collecte() != null) {
                            all_tax_accounts.add(taxe.getCompte_collecte());
                            account_to_type.put(taxe.getCompte_collecte(), "COLLECTE");
                        }
                        if (taxe.getCompte_deductible() != null) {
                            all_tax_accounts.add(taxe.getCompte_deductible());
                            account_to_type.put(taxe.getCompte_deductible(), "DEDUCTIBLE");
                        }
                    }

                    if (all_tax_accounts.isEmpty()) {
                        return Mono.error(new RuntimeException("No tax accounts configured in active taxes"));
                    }

                    // First, we need to map compte_id to no_compte
                    return compte_repository.findAllByOrganization_Id(organization_id)
                            .filter(c -> all_tax_accounts.contains(c.getNo_compte()))
                            .collectMap(Compte::getId, Compte::getNo_compte)
                            .flatMap(id_to_no -> {
                                return detail_repository
                                        .findByAccountNumbersAndDateRange(organization_id, all_tax_accounts, start, end)
                                        .collectList()
                                        .map(details -> {
                                            BigDecimal total_collecte = BigDecimal.ZERO;
                                            BigDecimal total_deductible = BigDecimal.ZERO;
                                            Map<String, BigDecimal> breakdown = new HashMap<>();

                                            for (DetailEcriture detail : details) {
                                                String account_no = id_to_no.get(detail.getCompte_id());
                                                if (account_no == null)
                                                    continue;

                                                String type = account_to_type.get(account_no);
                                                BigDecimal amount = detail.getSens() == Sens.DEBIT
                                                        ? detail.getMontant_debit()
                                                        : detail.getMontant_credit();

                                                if ("COLLECTE".equals(type)) {
                                                    // TVA Collectée is usually credit balance
                                                    total_collecte = total_collecte.add(amount);
                                                } else if ("DEDUCTIBLE".equals(type)) {
                                                    // TVA Déductible is usually debit balance
                                                    total_deductible = total_deductible.add(amount);
                                                }

                                                breakdown.merge(account_no, amount, BigDecimal::add);
                                            }

                                            BigDecimal net_amount = total_collecte.subtract(total_deductible);

                                            String json_breakdown = "{}";
                                            try {
                                                Map<String, Object> data = new HashMap<>();
                                                data.put("total_collecte", total_collecte);
                                                data.put("total_deductible", total_deductible);
                                                data.put("breakdown", breakdown);
                                                json_breakdown = object_mapper.writeValueAsString(data);
                                            } catch (JsonProcessingException e) {
                                                log.error("Error serializing declaration data", e);
                                            }

                                            return DeclarationFiscale.builder()
                                                    .organizationId(organization_id)
                                                    .type_declaration("TVA")
                                                    .periode_debut(start_date)
                                                    .periode_fin(end_date)
                                                    .montant_total(net_amount.doubleValue())
                                                    .date_generation(LocalDate.now())
                                                    .statut("DRAFT")
                                                    .donnees_declaration(json_breakdown)
                                                    .notes("Generated automatically from ledger scanning")
                                                    .created_at(LocalDateTime.now())
                                                    .updated_at(LocalDateTime.now())
                                                    .created_by("system")
                                                    .updated_by("system")
                                                    .build();
                                        });
                            });
                });
    }
}
