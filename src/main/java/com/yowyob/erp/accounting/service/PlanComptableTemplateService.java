package com.yowyob.erp.accounting.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.PlanComptableTemplateDto;
import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.common.service.ValidationService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing the accounting plan templates (official OHADA
 * references).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableTemplateService {

    private final PlanComptableTemplateRepository template_repository;
    private final ValidationService validation_service;

    /**
     * Creates a new accounting account template.
     */
    @Transactional
    public Mono<PlanComptableTemplateDto> createAccount(PlanComptableTemplateDto dto) {
        return ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                .flatMap(current_user -> {
                    log.info("Creating accounting template account {}", dto.getNumero());

                    try {
                        validation_service.validateAccountNumber(dto.getNumero());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

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

                    return template_repository.save(account)
                            .map(saved -> {
                                log.info("✅ Template account created: {} - {}", saved.getNumero(), saved.getLibelle());
                                return mapToDto(saved);
                            });
                });
    }

    /**
     * Retrieves all account templates.
     */
    public Flux<PlanComptableTemplateDto> getAllAccounts() {
        return template_repository.findAll()
                .map(this::mapToDto);
    }

    /**
     * Maps an entity to its DTO.
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
