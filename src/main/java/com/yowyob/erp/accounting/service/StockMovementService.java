package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.*;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.common.enums.Sens;
import com.yowyob.erp.common.service.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service to handle accounting stock movements.
 * Converts stock movements into accounting entries dynamic configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementService {

    private final EcritureComptableRepository ecritureRepository;
    private final TimeService timeService;
    // P.S. CompteRepository, JournalComptableRepository needed for real
    // implementation
    // For now, we simulate dynamic lookups or expect Objects to be fully populated

    /**
     * Records a stock movement and generates the corresponding accounting entry.
     * 
     * @param mouvement The stock movement data
     * @param tenant    The context tenant
     */
    @Transactional
    public void recordStockMovement(MouvementStockComptable mouvement, Tenant tenant) {
        log.info("Processing stock movement for Tenant: {}", tenant.getCode());

        // 1. Validate movement data
        if (mouvement.getQuantite() <= 0 || mouvement.getCout_unitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and Unit Cost must be positive");
        }

        // 2. Create Accounting Entry (EcritureComptable)
        EcritureComptable ecriture = EcritureComptable.builder()
                .tenant(tenant)
                .libelle(mouvement.get_description())
                // Use movement date as the accounting date
                .date_ecriture(mouvement.get_date())
                // .date_comptabilisation() does not exist, date_ecriture serves this purpose
                .journal(JournalComptable.builder().id(mouvement.get_journal_comptable_id()).build())
                // .exercice() does not exist directly on EcritureComptable (inferred via
                // Periode)
                .periode(null) // TODO: Lookup period based on date
                .validee(true) // Was .statut("VALIDATED"), changed to boolean validee=true
                .numero_ecriture("MVT-" + UUID.randomUUID().toString().substring(0, 8)) // Was .reference()
                .build();

        // 3. Generate Details (Entry Lines)
        // Using the logic already embedded in MouvementStockComptable
        List<DetailEcriture> details = mouvement.generate_ecriture_details(tenant, ecriture);

        ecriture.setDetails(details);

        // 4. Persist
        ecritureRepository.save(ecriture);
        log.info("Accounting entry created for stock movement: {}", ecriture.getNumero_ecriture());
    }
}
