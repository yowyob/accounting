
package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.CsvReleveBancaireService;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.ReleveBancaire;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;   
import org.springframework.web.multipart.MultipartFile;
import java.util.List; 

@RestController
@RequestMapping("/api/accounting/pointage")
@RequiredArgsConstructor
@Tag(name = "Pointage Bancaire", description = "Pointage automatique des opérations bancaires")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PointageController {

    private final CsvReleveBancaireService csvService;
    private final DetailEcritureRepository detailEcritureRepository;

    @PostMapping("/import")
    public ResponseEntity<String> importerReleve(MultipartFile file) {
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
       
       
        try {
            List<ReleveBancaire> operations = csvService.parseReleveBancaire(file); 
            int pointees = 0;
        for (ReleveBancaire op : operations) {  
            List<DetailEcriture> candidats = detailEcritureRepository.findByTenantIdAndMontantAndDateProche(
                tenant.getId(), 
                op.getMontant(), 
                op.getDateOperation(),
                op.getDateOperation().plusDays(1), 
                op.getDateOperation().plusDays(1));    

            if (!candidats.isEmpty()) {
                DetailEcriture d = candidats.get(0);
                d.setPointee(true);
                d.setReference_bancaire(op.getLibelle());
                pointees++;
            }
        }
        return ResponseEntity.ok(pointees + " opérations pointées automatiquement");
        } catch (Exception e) {
            // Log the error for debugging
            //logger.error("Error processing pointage operation", e); 
            
            // Return an error response to the client
            return ResponseEntity.ok("ERREUR LORS DU POINTAGE "+e.getMessage());
        }
        
    }
}