package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.CompteUseCase;

import com.yowyob.erp.accounting.infrastructure.web.dto.CompteDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.domain.model.Compte;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CompteRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalAuditRepository;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service for managing OHADA accounting accounts.
 * Compatible with R2DBC + Reactive Redis Cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService implements CompteUseCase {

        private final CompteRepository compte_repository;
        private final com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository detail_repository;
        private final ReactiveRedisTemplate<String, Object> redis_template;
        private final JournalAuditRepository audit_repository;
        private final KafkaMessageService kafka_service;

        private static final String CACHE_PREFIX = "compte:solde:";
        private static final String CACHE_ALL_PREFIX = "compte:all:";
        private static final String CACHE_BY_NO_COMPTE_PREFIX = "compte:nocompte:";

        /**
         * Creates a new accounting account.
         */
        @Transactional
        public Mono<CompteDto> createCompte(CompteDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .switchIfEmpty(Mono.error(new IllegalStateException("Organization ID is required")))
                                .flatMap(organization_id -> ReactiveOrganizationContext
                                                .getCurrentOrganizationAsOrganization()
                                                .flatMap(organization -> {
                                                        log.info("Creating account for organization {} with number {}",
                                                                        organization_id, dto.getNo_compte());

                                                        Compte compte = mapToEntity(dto);
                                                        compte.setId(UUID.randomUUID());
                                                        compte.setOrganizationId(organization_id);
                                                        compte.setCreated_at(LocalDateTime.now());
                                                        compte.setUpdated_at(LocalDateTime.now());
                                                        compte.setCreated_by("system");
                                                        compte.setUpdated_by("system");
                                                        compte.setActif(true);

                                                        return compte_repository.save(compte)
                                                                        .flatMap(saved -> ReactiveOrganizationContext
                                                                                        .getCurrentUser()
                                                                                        .defaultIfEmpty("system")
                                                                                        .flatMap(user -> redis_template
                                                                                                        .opsForValue()
                                                                                                        .set(CACHE_PREFIX
                                                                                                                        + organization_id
                                                                                                                        + ":"
                                                                                                                        + saved.getId(),
                                                                                                                        saved.getSolde())
                                                                                                        .then(logAudit(
                                                                                                                        organization,
                                                                                                                        user,
                                                                                                                        "COMPTE_CREATED",
                                                                                                                        "Creation of account: "
                                                                                                                                        + saved.getNo_compte()))
                                                                                                        .then(invalidateCache(
                                                                                                                        organization_id,
                                                                                                                        saved.getNo_compte()))
                                                                                                        .thenReturn(mapToDto(
                                                                                                                        saved))));
                                                }));
        }

        /**
         * Finds all accounts for a organization.
         */
        @SuppressWarnings("unchecked")
        public Flux<CompteDto> findAllByOrganization(UUID organization_id) {
                String cache_key = CACHE_ALL_PREFIX + organization_id;

                return redis_template.opsForValue().get(cache_key)
                                .cast(java.util.List.class)
                                .flatMapMany(list -> Flux.fromIterable((Iterable<CompteDto>) list))
                                .switchIfEmpty(
                                                compte_repository.findAllByOrganization_Id(organization_id)
                                                                .map(this::mapToDto)
                                                                .collectList()
                                                                .flatMap(list -> redis_template.opsForValue()
                                                                                .set(cache_key, list).thenReturn(list))
                                                                .flatMapMany(Flux::fromIterable));
        }

        /**
         * Finds an account by ID and organization ID.
         */
        public Mono<CompteDto> findById(UUID organization_id, UUID id) {
                return compte_repository.findByOrganization_IdAndId(organization_id, id)
                                .switchIfEmpty(Mono.error(
                                                new ResourceNotFoundException("Accounting account", id.toString())))
                                .flatMap(compte -> {
                                        String cache_key = CACHE_PREFIX + organization_id + ":" + id;
                                        return redis_template.opsForValue().get(cache_key)
                                                        .map(cached -> {
                                                                if (cached instanceof BigDecimal) {
                                                                        compte.setSolde((BigDecimal) cached);
                                                                } else if (cached instanceof Number) {
                                                                        compte.setSolde(new BigDecimal(
                                                                                        cached.toString()));
                                                                } else if (cached instanceof String) {
                                                                        compte.setSolde(new BigDecimal(
                                                                                        (String) cached));
                                                                }
                                                                return mapToDto(compte);
                                                        })
                                                        .switchIfEmpty(
                                                                        redis_template.opsForValue()
                                                                                        .set(cache_key, compte
                                                                                                        .getSolde())
                                                                                        .thenReturn(mapToDto(compte)));
                                });
        }

        /**
         * Finds accounts by account number. Matches exactly or by prefix.
         */
        public Flux<CompteDto> findByNoCompte(UUID organization_id, String no_compte) {
                return compte_repository.findByOrganization_IdAndNo_compte(organization_id, no_compte)
                                .map(this::mapToDto)
                                .flux();
        }

        /**
         * Gets all client accounts (411xxx).
         */
        public Flux<CompteDto> getClientAccounts(UUID organization_id) {
                log.debug("Retrieving client accounts for organization {}", organization_id);
                return compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(compte -> compte.getNo_compte() != null
                                                && compte.getNo_compte().startsWith("411"))
                                .map(this::mapToDto);
        }

        /**
         * Gets all supplier accounts (401xxx).
         */
        public Flux<CompteDto> getSupplierAccounts(UUID organization_id) {
                log.debug("Retrieving supplier accounts for organization {}", organization_id);
                return compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(compte -> compte.getNo_compte() != null
                                                && compte.getNo_compte().startsWith("401"))
                                .map(this::mapToDto);
        }

        /**
         * Gets all bank accounts (521xxx).
         */
        public Flux<CompteDto> getBankAccounts(UUID organization_id) {
                log.debug("Retrieving bank accounts for organization {}", organization_id);
                return compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(compte -> compte.getNo_compte() != null
                                                && compte.getNo_compte().startsWith("521"))
                                .map(this::mapToDto);
        }

        /**
         * Gets all cash accounts (571xxx).
         */
        public Flux<CompteDto> getCashAccounts(UUID organization_id) {
                log.debug("Retrieving cash accounts for organization {}", organization_id);
                return compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(compte -> compte.getNo_compte() != null
                                                && compte.getNo_compte().startsWith("571"))
                                .map(this::mapToDto);
        }

        /**
         * Gets accounts by type (ACTIF, PASSIF, CHARGE, PRODUIT, TAXE).
         */
        public Flux<CompteDto> getAccountsByType(UUID organization_id, String type) {
                log.debug("Retrieving {} accounts for organization {}", type, organization_id);
                return compte_repository.findAllByOrganization_Id(organization_id)
                                .filter(compte -> type.equalsIgnoreCase(compte.getType_compte()))
                                .map(this::mapToDto);
        }

        /**
         * Updates an existing account.
         */
        @Transactional
        public Mono<CompteDto> updateCompte(UUID organization_id, UUID id, CompteDto dto) {
                return compte_repository.findByOrganization_IdAndId(organization_id, id)
                                .switchIfEmpty(Mono.error(
                                                new ResourceNotFoundException("Accounting account", id.toString())))
                                .flatMap(compte -> {
                                        compte.setLibelle(dto.getLibelle());
                                        compte.setNotes(dto.getNotes());
                                        compte.setExternal_id(dto.getExternal_id());
                                        compte.setActif(dto.getActif() != null ? dto.getActif() : compte.getActif());
                                        compte.setNo_compte(dto.getNo_compte() != null ? dto.getNo_compte()
                                                        : compte.getNo_compte());
                                        compte.setClasse(
                                                        dto.getClasse() != null ? dto.getClasse() : compte.getClasse());
                                        compte.setType_compte(dto.getType_compte() != null ? dto.getType_compte()
                                                        : compte.getType_compte());
                                        compte.setUpdated_at(LocalDateTime.now());
                                        compte.setUpdated_by("system");

                                        compte.setNotNew();
                                        return compte_repository.save(compte)
                                                        .flatMap(updated -> ReactiveOrganizationContext
                                                                        .getCurrentUser()
                                                                        .defaultIfEmpty("system")
                                                                        .flatMap(user -> redis_template.opsForValue()
                                                                                        .set(CACHE_PREFIX + organization_id
                                                                                                        + ":"
                                                                                                        + updated.getId(),
                                                                                                        updated.getSolde())
                                                                                        .then(ReactiveOrganizationContext
                                                                                                        .getCurrentOrganizationAsOrganization()
                                                                                                        .flatMap(organization -> logAudit(
                                                                                                                        organization,
                                                                                                                        user,
                                                                                                                        "COMPTE_UPDATED",
                                                                                                                        "Update of account: "
                                                                                                                                        + updated.getNo_compte())))
                                                                                        .then(invalidateCache(
                                                                                                        organization_id,
                                                                                                        updated.getNo_compte()))
                                                                                        .thenReturn(mapToDto(updated))));
                                });
        }

        /**
         * Deletes an account by ID.
         */
        @Transactional
        public Mono<Void> deleteById(UUID organization_id, UUID id) {
                return compte_repository.findByOrganization_IdAndId(organization_id, id)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Account", id.toString())))
                                .flatMap(compte -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(user -> compte_repository.delete(compte)
                                                                .then(ReactiveOrganizationContext
                                                                                .getCurrentOrganizationAsOrganization()
                                                                                .flatMap(organization -> logAudit(
                                                                                                organization,
                                                                                                user,
                                                                                                "COMPTE_DELETED",
                                                                                                "Deletion of account: "
                                                                                                                + compte.getNo_compte())))
                                                                .then(invalidateCache(organization_id,
                                                                                compte.getNo_compte()))
                                                .doOnSuccess(v -> log.info("Account deleted: {} for organization {}",
                                                                compte.getNo_compte(),
                                                                organization_id)));
        }

        /**
         * Updates account balances based on a validated accounting entry.
         */
        @Transactional
        public Mono<Void> updateBalances(UUID organization_id, UUID ecriture_id) {
                log.info("Updating account balances for entry {} (Organization: {})", ecriture_id, organization_id);

                return detail_repository.findByOrganization_IdAndEcriture_Id(organization_id, ecriture_id)
                                .flatMap(detail -> compte_repository.findById(detail.getCompte_id())
                                                .flatMap(compte -> {
                                                        BigDecimal debit = detail.getMontant_debit() != null
                                                                        ? detail.getMontant_debit()
                                                                        : BigDecimal.ZERO;
                                                        BigDecimal credit = detail.getMontant_credit() != null
                                                                        ? detail.getMontant_credit()
                                                                        : BigDecimal.ZERO;

                                                        BigDecimal delta;
                                                        if ("ACTIF".equals(compte.getType_compte())
                                                                        || "CHARGE".equals(compte.getType_compte())) {
                                                                delta = debit.subtract(credit);
                                                        } else {
                                                                delta = credit.subtract(debit);
                                                        }

                                                        compte.setSolde(compte.getSolde().add(delta));
                                                        return compte_repository.save(compte)
                                                                        .flatMap(saved -> redis_template.opsForValue()
                                                                                        .set(CACHE_PREFIX
                                                                                                        + organization_id
                                                                                                        + ":"
                                                                                                        + saved.getId(),
                                                                                                        saved.getSolde())
                                                                                        .then(invalidateCache(
                                                                                                        organization_id,
                                                                                                        saved.getNo_compte())));
                                                }))
                                .then();
        }

        private Compte mapToEntity(CompteDto dto) {
                Compte compte = new Compte();
                compte.setNo_compte(dto.getNo_compte());
                compte.setLibelle(dto.getLibelle());
                compte.setNotes(dto.getNotes());
                compte.setType_compte(dto.getType_compte());
                compte.setClasse(dto.getClasse());
                compte.setActif(dto.getActif() != null ? dto.getActif() : true);
                compte.setSolde(dto.getSolde() != null ? dto.getSolde() : BigDecimal.ZERO);
                compte.setExternal_id(dto.getExternal_id());
                return compte;
        }

        private CompteDto mapToDto(Compte compte) {
                return CompteDto.builder()
                                .id(compte.getId())
                                .external_id(compte.getExternal_id())
                                .no_compte(compte.getNo_compte())
                                .libelle(compte.getLibelle())
                                .notes(compte.getNotes())
                                .type_compte(compte.getType_compte())
                                .classe(compte.getClasse())
                                .solde(compte.getSolde())
                                .actif(compte.getActif())
                                .organization_id(compte.getOrganizationId())
                                .created_at(compte.getCreated_at())
                                .updated_at(compte.getUpdated_at())
                                .build();
        }

        /**
         * Creates an account with auto-generated number based on type.
         * Supported types: CLIENT, SUPPLIER, EMPLOYEE, CASH, BANK, VAT_COLLECTED,
         * VAT_DEDUCTIBLE, STOCK.
         */
        @Transactional
        public Mono<CompteDto> createAutoGeneratedAccount(String name, String type, String notes, UUID externalId) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String prefix;
                                        int classe;
                                        String typeCompte;

                                        switch (type.toUpperCase()) {
                                                case "CLIENT":
                                                        prefix = "411";
                                                        classe = 4;
                                                        typeCompte = "ACTIF";
                                                        break;
                                                case "SUPPLIER":
                                                        prefix = "401";
                                                        classe = 4;
                                                        typeCompte = "PASSIF";
                                                        break;
                                                case "EMPLOYEE":
                                                        prefix = "421";
                                                        classe = 4;
                                                        typeCompte = "PASSIF";
                                                        break;
                                                case "CASH":
                                                        prefix = "571";
                                                        classe = 5;
                                                        typeCompte = "ACTIF";
                                                        break;
                                                case "BANK":
                                                        prefix = "521";
                                                        classe = 5;
                                                        typeCompte = "ACTIF";
                                                        break;
                                                case "VAT_COLLECTED":
                                                        prefix = "443";
                                                        classe = 4;
                                                        typeCompte = "PASSIF";
                                                        break;
                                                case "VAT_DEDUCTIBLE":
                                                        prefix = "445";
                                                        classe = 4;
                                                        typeCompte = "ACTIF";
                                                        break;
                                                case "STOCK":
                                                        prefix = "311";
                                                        classe = 3;
                                                        typeCompte = "ACTIF";
                                                        break;
                                                case "SALES":
                                                        prefix = "701";
                                                        classe = 7;
                                                        typeCompte = "PASSIF"; // OHADA: Sales are usually passive
                                                                               // (credits) but in terms of structure
                                                                               // it's a class 7 (Results)
                                                        break;
                                                case "PURCHASE":
                                                        prefix = "601";
                                                        classe = 6;
                                                        typeCompte = "ACTIF"; // OHADA: Purchases are usually active
                                                                              // (debits) but in terms of structure it's
                                                                              // a class 6 (Results)
                                                        break;
                                                default:
                                                        return Mono.error(new BusinessException(
                                                                        "Unsupported account type: " + type));
                                        }

                                        return generateNextAccountNumber(organization_id, prefix)
                                                        .flatMap(no_compte -> {
                                                                CompteDto dto = CompteDto.builder()
                                                                                .no_compte(no_compte)
                                                                                .libelle(name)
                                                                                .notes(notes)
                                                                                .type_compte(typeCompte)
                                                                                .classe(classe)
                                                                                .actif(true)
                                                                                .solde(BigDecimal.ZERO)
                                                                                .external_id(externalId)
                                                                                .build();

                                                                return createCompte(dto);
                                                        });
                                });
        }

        private Mono<String> generateNextAccountNumber(UUID organization_id, String prefix) {
                return compte_repository
                                .findTopByOrganization_IdAndNo_compteStartingWithOrderByNo_compteDesc(organization_id,
                                                prefix)
                                .map(lastAccount -> {
                                        String lastNo = lastAccount.getNo_compte();
                                        if (lastNo.length() > prefix.length()) {
                                                try {
                                                        String suffixStr = lastNo.substring(prefix.length());
                                                        int sequence = Integer.parseInt(suffixStr);
                                                        return prefix + String.format("%0" + suffixStr.length() + "d",
                                                                        sequence + 1);
                                                } catch (NumberFormatException e) {
                                                        log.warn("Could not parse account suffix for {}", lastNo);
                                                }
                                        }
                                        return prefix + "00001";
                                })
                                .defaultIfEmpty(prefix + "00001");
        }

        private Mono<Void> invalidateCache(UUID organization_id, String no_compte) {
                return redis_template.delete(CACHE_ALL_PREFIX + organization_id)
                                .then(redis_template
                                                .delete(CACHE_BY_NO_COMPTE_PREFIX + organization_id + ":" + no_compte))
                                .then()
                                .doOnSuccess(v -> log.debug("Cache invalidated for organization {} and account {}",
                                                organization_id,
                                                no_compte));
        }

        private Mono<Void> logAudit(Organization organization, String utilisateur, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization.getId())
                                .action(action)
                                .utilisateur(utilisateur)
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
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, organization.getId(), action);
                                });
        }
}