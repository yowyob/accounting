package com.yowyob.erp.accounting.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.PlanComptableTemplateDto;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.common.service.ValidationService;
import com.yowyob.erp.config.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableTemplateService {

    private final PlanComptableTemplateRepository repository;
    private final ValidationService validationService;



    /* ============================================================================
     * CREATE ACCOUNT
     * ========================================================================== */
    @Transactional
    public PlanComptableTemplateDto createAccount(PlanComptableTemplateDto dto) {

        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("🧾 Création du compte comptable {} pour le tenant {}", dto.getNumero(), "tenantId");

        // ✅ Validation du numéro de compte
        validationService.validateAccountNumber(dto.getNumero());

 

        // Création de l'entité
        PlanComptableTemplate account = new PlanComptableTemplate();
        account.setNumero(dto.getNumero());
        account.setClasse(Character.getNumericValue(dto.getNumero().charAt(0)));
        account.setLibelle(dto.getLibelle());
        account.setNotes(dto.getNotes());
        account.setActif(true);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account.setCreatedBy(currentUser);
        account.setUpdatedBy(currentUser);

        PlanComptableTemplate saved = repository.save(account);
        PlanComptableTemplateDto result = mapToDto(saved);


        log.info("✅ Template Compte comptable créé : {} - {}", saved.getNumero(), saved.getLibelle());

        return result;
    }

    /* ============================================================================
     * READ
     * ========================================================================== */
    public List<PlanComptableTemplateDto> getAllAccounts() {
 
        List<PlanComptableTemplateDto> comptes = repository.findAll()
                .stream().map(this::mapToDto).collect(Collectors.toList());
        return comptes;
    }

    /* ============================================================================
     * MAPPING
     * ========================================================================== */
    private PlanComptableTemplateDto mapToDto(PlanComptableTemplate entity) {
        return PlanComptableTemplateDto.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .libelle(entity.getLibelle())
                .classe(entity.getClasse())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
