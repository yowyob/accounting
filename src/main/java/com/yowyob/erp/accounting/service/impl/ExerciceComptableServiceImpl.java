package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;
import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.ExerciceComptableRepository;
import com.yowyob.erp.accounting.service.ExerciceComptableService;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ExerciceComptableService.
 * Handles fiscal year creation, range validation, and closure.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciceComptableServiceImpl implements ExerciceComptableService {

    private final ExerciceComptableRepository exercice_repository;

    @Override
    @Transactional
    public ExerciceComptableDto createExercice(ExerciceComptableDto exercice_dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Creating new fiscal year '{}' for tenant {}", exercice_dto.getCode(), tenant_id);

        validateDates(exercice_dto, null);
        checkOverlap(exercice_dto, tenant_id, null);

        ExerciceComptable exercice = ExerciceComptable.builder()
                .tenant(new Tenant(tenant_id))
                .code(exercice_dto.getCode())
                .libelle(exercice_dto.getLibelle())
                .date_debut(exercice_dto.getDate_debut())
                .date_fin(exercice_dto.getDate_fin())
                .cloture(false)
                .build();

        ExerciceComptable saved = exercice_repository.save(exercice);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciceComptableDto getExercice(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return exercice_repository.findById(id)
                .filter(e -> e.getTenant().getId().equals(tenant_id))
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("ExerciceComptable", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciceComptableDto> getAllExercices() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return exercice_repository.findByTenant(new Tenant(tenant_id)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciceComptableDto getActiveExerciceForDate(LocalDate date) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return exercice_repository.findActiveForDate(new Tenant(tenant_id), date)
                .map(this::mapToDto)
                .orElseThrow(() -> new BusinessException("No active fiscal year found for date " + date));
    }

    @Override
    @Transactional
    public ExerciceComptableDto updateExercice(UUID id, ExerciceComptableDto exercice_dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Updating fiscal year {} for tenant {}", id, tenant_id);

        ExerciceComptable exercice = exercice_repository.findById(id)
                .filter(e -> e.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("ExerciceComptable", id.toString()));

        if (exercice.getCloture()) {
            throw new BusinessException("Cannot update a closed fiscal year.");
        }

        validateDates(exercice_dto, id);
        checkOverlap(exercice_dto, tenant_id, id);

        exercice.setCode(exercice_dto.getCode());
        exercice.setLibelle(exercice_dto.getLibelle());
        exercice.setDate_debut(exercice_dto.getDate_debut());
        exercice.setDate_fin(exercice_dto.getDate_fin());

        ExerciceComptable updated = exercice_repository.save(exercice);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void closeExercice(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Closing fiscal year {} for tenant {}", id, tenant_id);

        ExerciceComptable exercice = exercice_repository.findById(id)
                .filter(e -> e.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("ExerciceComptable", id.toString()));

        exercice.setCloture(true);
        exercice_repository.save(exercice);
    }

    @Override
    @Transactional
    public void deleteExercice(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.warn("Deleting fiscal year {} for tenant {}", id, tenant_id);

        ExerciceComptable exercice = exercice_repository.findById(id)
                .filter(e -> e.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("ExerciceComptable", id.toString()));

        if (exercice.getCloture()) {
            throw new BusinessException("Cannot delete a closed fiscal year.");
        }

        exercice_repository.delete(exercice);
    }

    private void validateDates(ExerciceComptableDto dto, UUID current_id) {
        if (dto.getDate_debut().isAfter(dto.getDate_fin())) {
            throw new BusinessException("Start date cannot be after end date.");
        }
    }

    private void checkOverlap(ExerciceComptableDto dto, UUID tenant_id, UUID current_id) {
        List<ExerciceComptable> existing = exercice_repository.findByTenant(new Tenant(tenant_id));
        for (ExerciceComptable e : existing) {
            if (current_id != null && e.getId().equals(current_id))
                continue;

            boolean overlap = (dto.getDate_debut().isBefore(e.getDate_fin())
                    && dto.getDate_fin().isAfter(e.getDate_debut())) ||
                    dto.getDate_debut().equals(e.getDate_debut()) || dto.getDate_fin().equals(e.getDate_fin());

            if (overlap) {
                throw new BusinessException(
                        "The fiscal year dates overlap with an existing fiscal year: " + e.getCode());
            }
        }
    }

    private ExerciceComptableDto mapToDto(ExerciceComptable entity) {
        return ExerciceComptableDto.builder()
                .id(entity.getId())
                .tenant_id(entity.getTenant().getId())
                .code(entity.getCode())
                .libelle(entity.getLibelle())
                .date_debut(entity.getDate_debut())
                .date_fin(entity.getDate_fin())
                .cloture(entity.getCloture())
                .created_at(entity.getCreated_at())
                .updated_at(entity.getUpdated_at())
                .created_by(entity.getCreated_by())
                .build();
    }
}
