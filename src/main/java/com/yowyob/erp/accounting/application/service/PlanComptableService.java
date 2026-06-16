package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.PlanComptableUseCase;

import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.PlanComptableDto;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.PlanComptable;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OrganizationRepository;
import com.yowyob.erp.accounting.infrastructure.initialization.OperationComptableInitializationService;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.shared.application.service.ValidationService;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service for managing the accounting plan (Plan Comptable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableService implements PlanComptableUseCase {

        private final PlanComptableRepository account_repository;
        private final PlanComptableTemplateRepository template_repository;
        private final ValidationService validation_service;
        private final KafkaMessageService kafka_service;
        private final RedisService redis_service;
        private final JournalAuditRepository audit_repository;
        private final OrganizationRepository organization_repository;
        private final OperationComptableInitializationService accounting_provisioning;

        private static final String CACHE_ALL = "plancomptable:all:";
        private static final String CACHE_ACTIVE = "plancomptable:active:";
        private static final String CACHE_SINGLE = "plancomptable:id:";
        private static final String CACHE_PREFIX = "plancomptable:prefix:";
        private static final String CACHE_CLASS = "plancomptable:class:";

        @Transactional
        public Mono<Void> initializePlanComptableForOrganization(UUID organization_id) {
                log.info("Initializing accounting plan for organization: {}", organization_id);

                return provisionPlanComptableForOrganization(organization_id)
                                // Always (re)ensure the essential ledger accounts and the standard
                                // operation templates (ACHAT/VENTE/PAIEMENT) so the billing→accounting
                                // chain can generate entries for THIS organization — not only the default
                                // one — and so existing organizations get them retroactively. Idempotent.
                                .then(accounting_provisioning.provisionForOrganization(organization_id));
        }

        /**
         * Idempotently creates the full OHADA chart of accounts ({@link PlanComptable}) for the
         * organization from the loaded template, when it has none yet. Returns the number of accounts
         * created (0 if the plan was already present). Does NOT provision operation templates — call
         * {@code OperationComptableInitializationService#provisionForOrganization} for those.
         */
        public Mono<Long> provisionPlanComptableForOrganization(UUID organization_id) {
                return ensureOrganizationExists(organization_id)
                                .then(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
                                .flatMap(current_user -> account_repository.findByOrganization_Id(organization_id)
                                                .hasElements()
                                                .flatMap(planExists -> {
                                                        if (Boolean.TRUE.equals(planExists)) {
                                                                log.info("Plan comptable already present for organization {} — skipping creation.",
                                                                                organization_id);
                                                                return Mono.just(0L);
                                                        }
                                                        return createPlanFromTemplate(organization_id, current_user);
                                                }));
        }

        private Mono<Long> createPlanFromTemplate(UUID organization_id, String current_user) {
                return template_repository.findAll()
                                .collectList()
                                .flatMap(templates -> {
                                        if (templates.isEmpty()) {
                                                return Mono.error(new BusinessException(
                                                                "Le modèle OHADA n'est pas disponible. "
                                                                                + "Redémarrez le backend pour charger plan_comptable_ohada_713.csv."));
                                        }
                                        List<PlanComptable> accounts = templates.stream()
                                                        .map(template -> PlanComptable.builder()
                                                                        .organizationId(organization_id)
                                                                        .no_compte(template.getNumero())
                                                                        .classe(template.getClasse())
                                                                        .libelle(template.getLibelle())
                                                                        .notes(template.getNotes())
                                                                        .actif(true)
                                                                        .created_at(LocalDateTime.now())
                                                                        .updated_at(LocalDateTime.now())
                                                                        .created_by(current_user)
                                                                        .updated_by(current_user)
                                                                        .build())
                                                        .toList();
                                        return account_repository.saveAll(accounts)
                                                        .then()
                                                        .then(redis_service.delete(CACHE_ALL + organization_id))
                                                        .then(redis_service.delete(CACHE_ACTIVE + organization_id))
                                                        .thenReturn((long) accounts.size())
                                                        .doOnSuccess(n -> log.info(
                                                                        "Successfully initialized {} accounts for organization {}",
                                                                        n, organization_id));
                                });
        }

        /**
         * Garantit que la ligne parente existe dans la table {@code organizations}
         * avant tout insert contraint par une FK (ex. plans_comptables). L'organisation
         * provient du kernel et n'est pas forcément déjà provisionnée côté comptabilité ;
         * on crée alors une ligne minimale pour satisfaire la contrainte.
         */
        private Mono<Void> ensureOrganizationExists(UUID organization_id) {
                return organization_repository.existsById(organization_id)
                                .flatMap(exists -> {
                                        if (Boolean.TRUE.equals(exists)) {
                                                return Mono.empty();
                                        }
                                        log.info("Organization {} absente côté accounting, provisionnement d'une ligne minimale.",
                                                        organization_id);
                                        Organization organization = Organization.builder()
                                                        .id(organization_id)
                                                        .code("ORG-" + organization_id)
                                                        .name("Organization " + organization_id)
                                                        .created_at(LocalDateTime.now())
                                                        .updated_at(LocalDateTime.now())
                                                        .build();
                                        organization.setNew(true);
                                        return organization_repository.save(organization).then();
                                });
        }

        @Transactional
        public Mono<PlanComptableDto> createAccount(PlanComptableDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        log.info("Creating account {} for organization {}",
                                                                        dto.getNo_compte(), organization_id);

                                                        try {
                                                                validation_service.validateAccountNumber(
                                                                                dto.getNo_compte());
                                                        } catch (Exception e) {
                                                                return Mono.error(e);
                                                        }

                                                        return account_repository
                                                                        .existsByOrganization_IdAndNo_compte(organization_id,
                                                                                        dto.getNo_compte())
                                                                        .flatMap(exists -> {
                                                                                if (Boolean.TRUE.equals(exists)) {
                                                                                        return Mono.error(
                                                                                                        new BusinessException(
                                                                                                                        "Account already exists: "
                                                                                                                                        + dto.getNo_compte()));
                                                                                }

                                                                                PlanComptable account = PlanComptable
                                                                                                .builder()
                                                                                                .organizationId(organization_id)
                                                                                                .no_compte(dto.getNo_compte())
                                                                                                .classe(Character
                                                                                                                .getNumericValue(
                                                                                                                                dto.getNo_compte()
                                                                                                                                                .charAt(0)))
                                                                                                .libelle(dto.getLibelle())
                                                                                                .notes(dto.getNotes())
                                                                                                .actif(true)
                                                                                                .created_at(LocalDateTime
                                                                                                                .now())
                                                                                                .updated_at(LocalDateTime
                                                                                                                .now())
                                                                                                .created_by(current_user)
                                                                                                .updated_by(current_user)
                                                                                                .build();

                                                                                return account_repository.save(account)
                                                                                                .flatMap(saved -> logAudit(
                                                                                                                organization_id,
                                                                                                                current_user,
                                                                                                                "ACCOUNT_CREATED",
                                                                                                                "Creation of account: "
                                                                                                                                + saved.getNo_compte())
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                + organization_id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                                        });
                                                }));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAllAccounts() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_ALL + organization_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository.findByOrganization_Id(organization_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAllActiveAccounts() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_ACTIVE + organization_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByOrganization_IdAndActifTrue(organization_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        public Mono<PlanComptableDto> getAccountById(UUID id) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_SINGLE + organization_id + ":" + id;
                                        return redis_service.get(key, PlanComptableDto.class)
                                                        .switchIfEmpty(account_repository
                                                                        .findByOrganization_IdAndId(organization_id, id)
                                                                        .switchIfEmpty(Mono.error(
                                                                                        new ResourceNotFoundException(
                                                                                                        "Accounting account",
                                                                                                        id.toString())))
                                                                        .map(this::mapToDto)
                                                                        .flatMap(dto -> redis_service
                                                                                        .save(key, dto, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(dto)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAccountsByClass(Integer classe) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_CLASS + organization_id + ":" + classe;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByOrganization_IdAndClasse(organization_id, classe)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(20))
                                                                                        .thenReturn(list)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAccountsByPrefix(String prefix) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_PREFIX + organization_id + ":" + prefix;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByOrganization_IdAndNo_compteStartingWith(
                                                                                        organization_id, prefix)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(20))
                                                                                        .thenReturn(list)));
                                });
        }

        @Transactional
        public Mono<PlanComptableDto> updateAccount(UUID id, PlanComptableDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> account_repository
                                                                .findByOrganization_IdAndId(organization_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Accounting account", id.toString())))
                                                                .flatMap(account -> {
                                                                        account.setLibelle(dto.getLibelle());
                                                                        account.setNotes(dto.getNotes());
                                                                        account.setUpdated_at(LocalDateTime.now());
                                                                        account.setUpdated_by(current_user);

                                                                        return account_repository.save(account)
                                                                                        .flatMap(saved -> logAudit(
                                                                                                        organization_id,
                                                                                                        current_user,
                                                                                                        "ACCOUNT_UPDATED",
                                                                                                        "Update of account: "
                                                                                                                        + saved.getNo_compte())
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_SINGLE
                                                                                                                                        + organization_id
                                                                                                                                        + ":"
                                                                                                                                        + id))
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_ALL
                                                                                                                                        + organization_id))
                                                                                                        .thenReturn(mapToDto(
                                                                                                                        saved)));
                                                                })));
        }

        @Transactional
        public Mono<Void> deactivateAccount(UUID id) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> account_repository
                                                                .findByOrganization_IdAndId(organization_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Accounting account", id.toString())))
                                                                .flatMap(account -> {
                                                                        account.setActif(false);
                                                                        account.setUpdated_at(LocalDateTime.now());
                                                                        account.setUpdated_by(current_user);

                                                                        return account_repository.save(account)
                                                                                        .flatMap(saved -> logAudit(
                                                                                                        organization_id,
                                                                                                        current_user,
                                                                                                        "ACCOUNT_DEACTIVATED",
                                                                                                        "Deactivation of account: "
                                                                                                                        + saved.getNo_compte())
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_SINGLE
                                                                                                                                        + organization_id
                                                                                                                                        + ":"
                                                                                                                                        + id))
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_ACTIVE
                                                                                                                                        + organization_id))
                                                                                                        .then());
                                                                })));
        }

        private PlanComptableDto mapToDto(PlanComptable entity) {
                return PlanComptableDto.builder()
                                .id(entity.getId())
                                .no_compte(entity.getNo_compte())
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

        private Mono<Void> logAudit(UUID organization_id, String user, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization_id)
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
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .id(saved.getId())
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, organization_id, action);
                                });
        }

        @SuppressWarnings("unused")
        private Mono<Void> logAudit(Organization organization, String user, String action, String details) {
                return logAudit(organization.getId(), user, action, details);
        }
}
