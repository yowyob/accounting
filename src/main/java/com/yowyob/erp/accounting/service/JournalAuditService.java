package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing audit logs.
 * Handles mapping between JournalAudit entity and JournalAuditDto.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
public class JournalAuditService {

    private final JournalAuditRepository audit_repository;

    /**
     * Retrieves all audits for a tenant, ordered by action date descending.
     * 
     * @param tenant_id the tenant ID
     * @param limit     maximum number of results
     * @return list of audit DTOs
     */
    public List<JournalAuditDto> getAllByTenant(UUID tenant_id, int limit) {
        return audit_repository.findByTenant_IdOrderByDate_actionDesc(tenant_id)
                .stream()
                .limit(limit)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves audits for a tenant within a specific time range.
     * 
     * @param tenant_id  the tenant ID
     * @param start_date start of the period
     * @param end_date   end of the period
     * @return list of audit DTOs
     */
    public List<JournalAuditDto> getByPeriode(UUID tenant_id, LocalDateTime start_date, LocalDateTime end_date) {
        return audit_repository.findByTenant_IdAndDate_actionBetween(tenant_id, start_date, end_date)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves audits for a tenant filtered by user.
     * 
     * @param tenant_id   the tenant ID
     * @param utilisateur the user name
     * @return list of audit DTOs
     */
    public List<JournalAuditDto> getByUtilisateur(UUID tenant_id, String utilisateur) {
        return audit_repository.findByTenant_IdAndUtilisateur(tenant_id, utilisateur)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves audits for a tenant filtered by action.
     * 
     * @param tenant_id the tenant ID
     * @param action    the action type
     * @return list of audit DTOs
     */
    public List<JournalAuditDto> getByAction(UUID tenant_id, String action) {
        return audit_repository.findByTenant_IdAndAction(tenant_id, action)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves audits related to a specific accounting entry.
     * 
     * @param tenant_id   the tenant ID
     * @param ecriture_id the entry ID
     * @return list of audit DTOs
     */
    public List<JournalAuditDto> getByEcriture(UUID tenant_id, UUID ecriture_id) {
        return audit_repository.findByTenant_IdAndEcriture_comptable_id(tenant_id, ecriture_id)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps a JournalAudit entity to its DTO.
     * 
     * @param entity the entity to map
     * @return the mapped DTO
     */
    private JournalAuditDto mapToDto(JournalAudit entity) {
        if (entity == null)
            return null;
        return JournalAuditDto.builder()
                .id(entity.getId())
                .ecriture_comptable_id(entity.getEcriture_comptable_id())
                .action(entity.getAction())
                .date_action(entity.getDate_action())
                .utilisateur(entity.getUtilisateur())
                .details(entity.getDetails())
                .adresse_ip(entity.getAdresse_ip())
                .donnees_avant(entity.getDonnees_avant())
                .donnees_apres(entity.getDonnees_apres())
                .created_at(entity.getCreated_at())
                .updated_at(entity.getUpdated_at())
                .created_by(entity.getCreated_by())
                .updated_by(entity.getUpdated_by())
                .build();
    }
}
