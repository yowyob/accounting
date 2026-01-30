package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service for managing audit logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalAuditService {

    private final JournalAuditRepository audit_repository;

    /**
     * Retrieves all audits for a tenant, ordered by action date descending.
     */
    public Flux<JournalAuditDto> getAllByTenant(UUID tenant_id, int limit) {
        return audit_repository.findByTenant_IdOrderByDate_actionDesc(tenant_id)
                .take(limit)
                .map(this::mapToDto);
    }

    /**
     * Retrieves audits for a tenant within a specific time range.
     */
    public Flux<JournalAuditDto> getByPeriode(UUID tenant_id, LocalDateTime start_date, LocalDateTime end_date) {
        return audit_repository.findByTenant_IdAndDate_actionBetween(tenant_id, start_date, end_date)
                .map(this::mapToDto);
    }

    /**
     * Retrieves audits for a tenant filtered by user.
     */
    public Flux<JournalAuditDto> getByUtilisateur(UUID tenant_id, String utilisateur) {
        return audit_repository.findByTenant_IdAndUtilisateur(tenant_id, utilisateur)
                .map(this::mapToDto);
    }

    /**
     * Retrieves audits for a tenant filtered by action.
     */
    public Flux<JournalAuditDto> getByAction(UUID tenant_id, String action) {
        return audit_repository.findByTenant_IdAndAction(tenant_id, action)
                .map(this::mapToDto);
    }

    /**
     * Retrieves audits related to a specific accounting entry.
     */
    public Flux<JournalAuditDto> getByEcriture(UUID tenant_id, UUID ecriture_id) {
        return audit_repository.findByTenant_IdAndEcriture_comptable_id(tenant_id, ecriture_id)
                .map(this::mapToDto);
    }

    /**
     * Maps a JournalAudit entity to its DTO.
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
