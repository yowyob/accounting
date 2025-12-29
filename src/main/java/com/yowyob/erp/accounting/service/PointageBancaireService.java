package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.ReleveBancaire;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PointageBancaireService {

    private final DetailEcritureRepository detailRepo;
    private final CsvReleveBancaireService csvService;  

    public int importerEtPointer(MultipartFile file) throws Exception {
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        List<ReleveBancaire> releve = csvService.parseReleveBancaire(file);

        int pointees = 0;
        for (ReleveBancaire op : releve) {
            LocalDate debut = op.getDateOperation();
            LocalDate fin = op.getDateOperation().plusDays(1);
            LocalDate ref = op.getDateOperation();

            List<DetailEcriture> candidats = detailRepo.findByTenantIdAndMontantAndDateProche(
                tenant.getId(), op.getMontant(),   debut, fin, ref
            );

            if (!candidats.isEmpty()) {
                DetailEcriture de = candidats.get(0);
                de.setPointee(true);
                de.setReference_bancaire("AUTO → " + op.getLibelle().substring(0, Math.min(50, op.getLibelle().length())));
                pointees++;
            }
        }
        return pointees;
    }
}