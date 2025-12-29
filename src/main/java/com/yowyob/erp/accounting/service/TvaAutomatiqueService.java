package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.common.enums.Sens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service for applying automatic VAT calculations on accounting entries.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
public class TvaAutomatiqueService {

    private final CompteRepository compte_repository;

    /**
     * Applies VAT to an accounting entry based on sales accounts (class 70).
     * 
     * @param ecriture the accounting entry
     */
    @Transactional
    public void appliquerTvaSurEcriture(EcritureComptable ecriture) {
        BigDecimal base_vente = ecriture.getDetails().stream()
                .filter(d -> d.getCompte() != null && d.getCompte().getNo_compte().startsWith("70"))
                .map(d -> d.getMontant_credit() != null ? d.getMontant_credit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (base_vente.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tva = base_vente.multiply(new BigDecimal("19.25")).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);

            Compte compte_445 = compte_repository.findByTenant_IdAndNo_compte(ecriture.getTenant().getId(), "445000")
                    .orElseThrow(() -> new RuntimeException("Account 445000 missing"));

            DetailEcriture ligne_tva = DetailEcriture.builder()
                    .ecriture(ecriture)
                    .tenant(ecriture.getTenant())
                    .compte(compte_445)
                    .libelle("Automatic 19.25% VAT")
                    .sens(Sens.CREDIT)
                    .montant_credit(tva)
                    .montant_debit(BigDecimal.ZERO)
                    .date_ecriture(LocalDateTime.now())
                    .created_at(LocalDateTime.now())
                    .updated_at(LocalDateTime.now())
                    .created_by("system")
                    .updated_by("system")
                    .build();

            ecriture.getDetails().add(ligne_tva);
        }
    }
}