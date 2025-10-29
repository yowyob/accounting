package com.yowyob.erp.accounting.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.PeriodeComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeriodeComptableService {

    private final PeriodeComptableRepository periodeRepository;
    private final JournalAuditRepository auditRepository;
    private final Validator validator;
    private final KafkaMessageService kafkaMessageService;
    private final RedisService redisService;

    private static final String CACHE_ALL = "periodes:all:";
    private static final String CACHE_ACTIVE = "periodes:active:";
    private static final String CACHE_SINGLE = "periode:";
    private static final String CACHE_CURRENT = "periode:current:";

    /* ============================================================================
     * CREATE
     * ========================================================================== */
    @Transactional
    public PeriodeComptableDto createPeriode(PeriodeComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        log.info("🧾 Création d’une période comptable [{} - {}] pour le tenant {}", dto.getCode(), dto.getDateDebut(), tenantId);
        validateDto(dto);

        if (periodeRepository.findByTenant_IdAndCode(tenantId, dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Le code de période existe déjà : " + dto.getCode());
        }

        validateNoOverlap(tenantId, dto.getDateDebut(), dto.getDateFin(), null);

        PeriodeComptable entity = mapToEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);

        //PeriodeComptable saved = periodeRepository.save(entity);
        PeriodeComptableDto result = null; //= mapToDto(saved);

        PeriodeComptable savedPeriode;
        try {
            // 🛑 Tentative de sauvegarde de la période comptable
            savedPeriode = periodeRepository.save(entity);
             kafkaMessageService.sendAuditLog(result, tenantId.toString(), "PERIODE_CREATED");
            logAudit(tenantId, user, "CREATE", "Création période : " + dto.getCode());
            redisService.delete(CACHE_ALL + tenantId);
        } catch (Exception e) {
            // 🛑 LOG CRITIQUE : Capture l'exception exacte pour identifier la contrainte violée.
            log.error("🛑  Erreur critique lors de la sauvegarde de PeriodeComptable (transaction échouée) : {}", e.getMessage(), e);
            throw e;
        }
       

        return result;
    }

    /* ============================================================================
     * READ
     * ========================================================================== */
    public Optional<PeriodeComptableDto> getPeriode(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_SINGLE + tenantId + ":" + id;

        PeriodeComptableDto cached = redisService.get(cacheKey, PeriodeComptableDto.class);
        if (cached != null) return Optional.of(cached);

        PeriodeComptable periode = periodeRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Période comptable", id.toString()));

        PeriodeComptableDto dto = mapToDto(periode);
        redisService.save(cacheKey, dto, Duration.ofMinutes(15));
        return Optional.of(dto);
    }

    public List<PeriodeComptableDto> getAllPeriodes() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_ALL + tenantId;

        List<PeriodeComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PeriodeComptableDto> periodes = periodeRepository.findByTenant_IdOrderByDateDebutDesc(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redisService.save(cacheKey, periodes, Duration.ofMinutes(10));
        return periodes;
    }

    public Optional<PeriodeComptableDto> getByCode(String code) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return periodeRepository.findByTenant_IdAndCode(tenantId, code).map(this::mapToDto);
    }

    public Optional<PeriodeComptableDto> getByDate(LocalDate date) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return periodeRepository.findByTenant_IdAndDateInRange(tenantId, date).map(this::mapToDto);
    }

    public List<PeriodeComptableDto> getNonClosedPeriodes() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String cacheKey = CACHE_ACTIVE + tenantId;

        List<PeriodeComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<PeriodeComptableDto> periodes = periodeRepository.findByTenant_IdAndClotureeFalse(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());

        redisService.save(cacheKey, periodes, Duration.ofMinutes(10));
        return periodes;
    }

    public List<PeriodeComptableDto> getByRange(LocalDate start, LocalDate end) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return periodeRepository.findByTenant_IdAndPeriodRange(tenantId, start, end)
                .stream().map(this::mapToDto).toList();
    }

    /* ✅ NOUVELLE MÉTHODE : Get Current Period */
    public PeriodeComptableDto getCurrentPeriode(UUID tenantId) {
        String cacheKey = CACHE_CURRENT + tenantId;

        PeriodeComptableDto cached = redisService.get(cacheKey, PeriodeComptableDto.class);
        if (cached != null) return cached;

        LocalDate today = LocalDate.now();
        PeriodeComptable periode = periodeRepository.findByTenant_IdAndDateInRange(tenantId, today)
                .filter(p -> !Boolean.TRUE.equals(p.getCloturee()))
                .orElseThrow(() -> new ResourceNotFoundException("Aucune période comptable ouverte pour la date actuelle"));

        PeriodeComptableDto dto = mapToDto(periode);
        redisService.save(cacheKey, dto, Duration.ofMinutes(30));

        log.info("📅 Période comptable courante pour le tenant {} : {}", tenantId, dto.getCode());
        return dto;
    }

    /* ============================================================================
     * UPDATE
     * ========================================================================== */
    @Transactional
    public PeriodeComptableDto updatePeriode(UUID id, PeriodeComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable existing = periodeRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Période comptable", id.toString()));

        if (Boolean.TRUE.equals(existing.getCloturee())) {
            throw new IllegalStateException("Impossible de modifier une période clôturée");
        }

        if (!existing.getCode().equals(dto.getCode()) &&
                periodeRepository.findByTenant_IdAndCode(tenantId, dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Code période déjà existant : " + dto.getCode());
        }

        validateNoOverlap(tenantId, dto.getDateDebut(), dto.getDateFin(), id);
        validateDto(dto);

        existing.setCode(dto.getCode());
        existing.setDateDebut(dto.getDateDebut());
        existing.setDateFin(dto.getDateFin());
        existing.setNotes(dto.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(user);

        PeriodeComptable saved = periodeRepository.save(existing);
        PeriodeComptableDto result = mapToDto(saved);

        kafkaMessageService.sendAuditLog(result, tenantId.toString(), "PERIODE_UPDATED");
        logAudit(tenantId, user, "UPDATE", "Mise à jour de la période : " + dto.getCode());
        redisService.delete(CACHE_ALL + tenantId);
        redisService.delete(CACHE_SINGLE + tenantId + ":" + id);

        return result;
    }

    /* ============================================================================
     * CLOSE & DELETE (inchangé)
     * ========================================================================== */
    @Transactional
    public PeriodeComptableDto closePeriode(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable periode = periodeRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Période comptable", id.toString()));

        if (Boolean.TRUE.equals(periode.getCloturee())) {
            throw new IllegalStateException("Période déjà clôturée");
        }

        periode.setCloturee(true);
        periode.setDateCloture(LocalDate.now());
        periode.setUpdatedBy(user);
        periode.setUpdatedAt(LocalDateTime.now());

        PeriodeComptable saved = periodeRepository.save(periode);
        PeriodeComptableDto result = mapToDto(saved);

        kafkaMessageService.sendAuditLog(result, tenantId.toString(), "PERIODE_CLOSED");
        logAudit(tenantId, user, "CLOSE", "Clôture de la période : " + periode.getCode());
        redisService.delete(CACHE_ALL + tenantId);
        redisService.delete(CACHE_ACTIVE + tenantId);

        return result;
    }

    @Transactional
    public void deletePeriode(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        PeriodeComptable periode = periodeRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Période comptable", id.toString()));

        if (Boolean.TRUE.equals(periode.getCloturee())) {
            throw new IllegalStateException("Impossible de supprimer une période clôturée");
        }

        periodeRepository.delete(periode);
        kafkaMessageService.sendAuditLog(periode, tenantId.toString(), "PERIODE_DELETED");
        logAudit(tenantId, user, "DELETE", "Suppression de la période : " + periode.getCode());
        redisService.delete(CACHE_ALL + tenantId);
        redisService.delete(CACHE_SINGLE + tenantId + ":" + id);
    }

    /* ============================================================================
     * HELPERS
     * ========================================================================== */
    private void validateDto(PeriodeComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        if (dto.getDateFin().isBefore(dto.getDateDebut()))
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
    }

    private void validateNoOverlap(UUID tenantId, LocalDate debut, LocalDate fin, UUID excludeId) {
        List<PeriodeComptable> existing = periodeRepository.findByTenant_IdOrderByDateDebutDesc(tenantId);
        for (PeriodeComptable p : existing) {
            if (excludeId != null && p.getId().equals(excludeId)) continue;
            if (!(fin.isBefore(p.getDateDebut()) || debut.isAfter(p.getDateFin()))) {
                throw new IllegalArgumentException("Période chevauchante détectée avec : " + p.getCode());
            }
        }
    }

    private PeriodeComptable mapToEntity(PeriodeComptableDto dto) {
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();

        // LOG CRITIQUE AJOUTÉ POUR LE DÉBOGAGE
        if (tenant == null) {
            log.error("TenantContext n'a pas pu fournir une entité Tenant valide.");
            throw new IllegalStateException("TenantContext n'a pas pu fournir une entité Tenant valide.");
        }
        // Ce log.debug n'est visible que si le niveau de log est DEBUG
         log.debug("Tenant ID récupéré du contexte : {}", tenant.getId()); 
        PeriodeComptable p = new PeriodeComptable();
        p.setTenant(tenant);
        p.setCode(dto.getCode());
        p.setDateDebut(dto.getDateDebut());
        p.setDateFin(dto.getDateFin());
        p.setCloturee(Optional.ofNullable(dto.getCloturee()).orElse(false));
        p.setDateCloture(dto.getDateCloture());
        p.setNotes(dto.getNotes());
        return p;
    }

    private PeriodeComptableDto mapToDto(PeriodeComptable p) {
        return PeriodeComptableDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .dateDebut(p.getDateDebut())
                .dateFin(p.getDateFin())
                .cloturee(p.getCloturee())
                .dateCloture(p.getDateCloture())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    private void logAudit(UUID tenantId, String user, String action, String details) {
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();

        JournalAudit audit = new JournalAudit();
        audit.setTenant(tenant);
        audit.setUtilisateur(user);
        audit.setAction(action);
        audit.setDetails(details);
        audit.setDateAction(LocalDateTime.now());
        auditRepository.save(audit);
        kafkaMessageService.sendAuditLog(audit, tenantId.toString(), action);
    }
}
