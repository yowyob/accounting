package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.OrganizationDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import com.yowyob.erp.accounting.service.OrganizationService;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Implementation of OrganizationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

        private final OrganizationRepository organization_repository;
        private final JournalAuditRepository audit_repository;
        private final KafkaMessageService kafka_service;

        @Override
        @Transactional
        public Mono<OrganizationDto> createOrganization(OrganizationDto organization_dto) {
                log.info("Creating new organization: {}", organization_dto.getName());
                Organization organization = Organization.builder()
                                .name(organization_dto.getName())
                                .description(organization_dto.getDescription())
                                .address(organization_dto.getAddress())
                                .tax_id(organization_dto.getTax_id())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .build();

                return organization_repository.save(organization)
                                .flatMap(saved -> logAudit(saved.getId(), "ORGANIZATION_CREATED",
                                                "Organization created: " + saved.getName())
                                                .thenReturn(mapToDto(saved)));
        }

        @Override
        public Mono<OrganizationDto> getOrganization(UUID id) {
                return organization_repository.findById(id)
                                .map(this::mapToDto)
                                .switchIfEmpty(Mono
                                                .error(new ResourceNotFoundException("Organization", id.toString())));
        }

        @Override
        public Flux<OrganizationDto> getAllOrganizations() {
                return organization_repository.findAll()
                                .map(this::mapToDto);
        }

        @Override
        @Transactional
        public Mono<OrganizationDto> updateOrganization(UUID id, OrganizationDto organization_dto) {
                log.info("Updating organization ID: {}", id);
                return organization_repository.findById(id)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Organization", id.toString())))
                                .flatMap(organization -> {
                                        organization.setName(organization_dto.getName());
                                        organization.setDescription(organization_dto.getDescription());
                                        organization.setAddress(organization_dto.getAddress());
                                        organization.setTax_id(organization_dto.getTax_id());
                                        organization.setUpdated_at(LocalDateTime.now());

                                        return organization_repository.save(organization)
                                                        .flatMap(saved -> logAudit(saved.getId(),
                                                                        "ORGANIZATION_UPDATED",
                                                                        "Organization updated: " + saved.getName())
                                                                        .thenReturn(mapToDto(saved)));
                                });
        }

        @Override
        @Transactional
        public Mono<Void> deleteOrganization(UUID id) {
                log.warn("Deleting organization ID: {}", id);
                return organization_repository.findById(id)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Organization", id.toString())))
                                .flatMap(org -> organization_repository.deleteById(id)
                                                .then(logAudit(id, "ORGANIZATION_DELETED",
                                                                "Organization deleted: " + org.getName())));
        }

        private OrganizationDto mapToDto(Organization organization) {
                return OrganizationDto.builder()
                                .id(organization.getId())
                                .name(organization.getName())
                                .description(organization.getDescription())
                                .address(organization.getAddress())
                                .tax_id(organization.getTax_id())
                                .created_at(organization.getCreated_at())
                                .updated_at(organization.getUpdated_at())
                                .build();
        }

        private Mono<Void> logAudit(UUID organizationId, String action, String details) {
                // Since Organization operations might be outside tenant context context or
                // global
                // We do our best to log. If tenant context is available we use it.
                // For Organization management, it might be system admin level.
                return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> ReactiveTenantContext.getTenantId()
                                                // If no tenant context, we might use a null tenantId or handle global
                                                // events
                                                // differently
                                                // For now assuming organization mgmt happens within a context or we
                                                // pass null
                                                .defaultIfEmpty(organizationId) // Fallback to org ID as context if
                                                                                // possible or null
                                                .flatMap(tenantId -> {
                                                        JournalAudit audit = JournalAudit.builder()
                                                                        .id(UUID.randomUUID())
                                                                        .tenantId(tenantId) // May be organization ID
                                                                                            // itself if it's the root
                                                                                            // context
                                                                        .action(action)
                                                                        .utilisateur(user)
                                                                        .details(details)
                                                                        .date_action(LocalDateTime.now())
                                                                        .created_at(LocalDateTime.now())
                                                                        .updated_at(LocalDateTime.now())
                                                                        .created_by("system")
                                                                        .updated_by("system")
                                                                        .build();

                                                        return audit_repository.save(audit)
                                                                        .flatMap(saved -> {
                                                                                JournalAuditDto auditDto = JournalAuditDto
                                                                                                .builder()
                                                                                                .action(saved.getAction())
                                                                                                .utilisateur(saved
                                                                                                                .getUtilisateur())
                                                                                                .details(saved.getDetails())
                                                                                                .date_action(saved
                                                                                                                .getDate_action())
                                                                                                .build();

                                                                                return kafka_service.sendAuditLog(
                                                                                                auditDto, tenantId,
                                                                                                action);
                                                                        });
                                                }));
        }
}
