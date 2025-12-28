package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import com.yowyob.erp.common.enums.Sens;    


/**
 * Service for applying automatic VAT.
 */
@Service
@RequiredArgsConstructor
public class TvaAutomatiqueService {

    private final CompteRepository compteRepo;

    @Transactional
    public void appliquerTvaSurEcriture(EcritureComptable ecriture) {
        BigDecimal baseVente = ecriture.getDetails().stream()
            .filter(d -> d.getCompte() != null && d.getCompte().getNoCompte().startsWith("70"))
            .map(d -> d.getMontantCredit() != null ? d.getMontantCredit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (baseVente.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tva = baseVente.multiply(new BigDecimal("19.25")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            Compte compte445 = compteRepo.findByTenant_IdAndNoCompte(ecriture.getTenant().getId(),"445000")
                .orElseThrow(() -> new RuntimeException("Compte 445000 absent"));

            DetailEcriture ligneTva = DetailEcriture.builder()
                .ecriture(ecriture)
                .tenant(ecriture.getTenant())
                .compte(compte445)
                .libelle("TVA 19,25% automatique")
                .sens(Sens.CREDIT)
                .montantCredit(tva)
                .dateEcriture(LocalDateTime.now())
                .build();

            ecriture.getDetails().add(ligneTva);
        }
    }
}