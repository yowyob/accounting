package com.yowyob.erp.accounting.service;

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

/**
 * Service for managing the accounting plan templates (official OHADA
 * references).
 * Provides functionality to create and retrieve template accounts used across
 * all tenants.
 * Follows snake_case naming and English Javadoc as per development charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableTemplateService {

    private final PlanComptableTemplateRepository template_repository;
    private final ValidationService validation_service;

    /**
     * Creates a new accounting account template.
     * 
     * @param dto the template data
     * @return the created template DTO
     */
    @Transactional
    public PlanComptableTemplateDto createAccount(PlanComptableTemplateDto dto) {
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("Creating accounting template account {}", dto.getNumero());

        validation_service.validateAccountNumber(dto.getNumero());

        PlanComptableTemplate account = PlanComptableTemplate.builder()
                .numero(dto.getNumero())
                .classe(Character.getNumericValue(dto.getNumero().charAt(0)))
                .libelle(dto.getLibelle())
                .notes(dto.getNotes())
                .actif(true)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(current_user)
                .updated_by(current_user)
                .build();

        PlanComptableTemplate saved = template_repository.save(account);
        PlanComptableTemplateDto result = mapToDto(saved);

        log.info("✅ Template account created: {} - {}", saved.getNumero(), saved.getLibelle());

        return result;
    }

    /**
     * Retrieves all account templates.
     * 
     * @return list of template DTOs
     */
    public List<PlanComptableTemplateDto> getAllAccounts() {
        return template_repository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps an entity to its DTO.
     * 
     * @param entity the entity to map
     * @return the mapped DTO
     */
    private PlanComptableTemplateDto mapToDto(PlanComptableTemplate entity) {
        return PlanComptableTemplateDto.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .libelle(entity.getLibelle())
                .classe(entity.getClasse())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .created_at(entity.getCreated_at())
                .updated_at(entity.getUpdated_at())
                .created_by(entity.getCreated_by())
                .updated_by(entity.getUpdated_by())
                .build();
    }
}
