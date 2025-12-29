package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.CompteDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.TenantRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing OHADA accounting accounts.
 * Compatible with PostgreSQL + Redis Cache.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService {

    private final CompteRepository compte_repository;
    private final RedisTemplate<String, Object> redis_template;
    private final TenantRepository tenant_repository;

    private static final String CACHE_PREFIX = "compte:solde:";
    private static final String CACHE_ALL_PREFIX = "compte:all:";
    private static final String CACHE_BY_NO_COMPTE_PREFIX = "compte:nocompte:";

    /**
     * Creates a new accounting account.
     * 
     * @param dto the account data
     * @return the created account DTO
     * @throws IllegalStateException if tenant ID is missing
     */
    @Transactional
    public CompteDto createCompte(CompteDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        if (tenant_id == null) {
            throw new IllegalStateException("Tenant ID is required");
        }
        log.info("Creating account for tenant {} with number {}", tenant_id, dto.getNo_compte());

        Compte compte = mapToEntity(dto);
        compte.setTenant(tenant_repository.findById(tenant_id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenant_id.toString())));
        compte.setCreated_at(LocalDateTime.now());
        compte.setUpdated_at(LocalDateTime.now());
        compte.setCreated_by("system");
        compte.setUpdated_by("system");
        compte.setSolde(BigDecimal.ZERO);
        compte.setActif(true);

        Compte saved = compte_repository.save(compte);

        // Cache the balance
        redis_template.opsForValue().set(CACHE_PREFIX + tenant_id + ":" + saved.getId(), saved.getSolde());
        invalidateCache(tenant_id, saved.getNo_compte());

        return mapToDto(saved);
    }

    /**
     * Finds all accounts for a tenant.
     * 
     * @param tenant_id the tenant ID
     * @return list of account DTOs
     */
    public List<CompteDto> findAllByTenant(UUID tenant_id) {
        String cache_key = CACHE_ALL_PREFIX + tenant_id;
        @SuppressWarnings("unchecked")
        List<CompteDto> comptes = (List<CompteDto>) redis_template.opsForValue().get(cache_key);

        if (comptes == null) {
            comptes = compte_repository.findAllByTenant_Id(tenant_id)
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            redis_template.opsForValue().set(cache_key, comptes);
        }
        return comptes;
    }

    /**
     * Finds an account by ID and tenant ID.
     * 
     * @param tenant_id the tenant ID
     * @param id        the account ID
     * @return the account DTO
     * @throws ResourceNotFoundException if account is not found
     */
    public Optional<CompteDto> findById(UUID tenant_id, UUID id) {
        Compte compte = compte_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));
        String cache_key = CACHE_PREFIX + tenant_id + ":" + id;

        BigDecimal cached_solde = (BigDecimal) redis_template.opsForValue().get(cache_key);
        if (cached_solde != null) {
            compte.setSolde(cached_solde);
        } else {
            redis_template.opsForValue().set(cache_key, compte.getSolde());
        }

        return Optional.of(mapToDto(compte));
    }

    /**
     * Finds accounts by account number.
     * 
     * @param tenant_id the tenant ID
     * @param no_compte the account number
     * @return list of account DTOs
     */
    public List<CompteDto> findByNoCompte(UUID tenant_id, String no_compte) {
        String cache_key = CACHE_BY_NO_COMPTE_PREFIX + tenant_id + ":" + no_compte;
        @SuppressWarnings("unchecked")
        List<CompteDto> cached = (List<CompteDto>) redis_template.opsForValue().get(cache_key);

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<CompteDto> comptes = compte_repository.findByTenant_IdAndNo_compte(tenant_id, no_compte)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        redis_template.opsForValue().set(cache_key, comptes);
        return comptes;
    }

    /**
     * Updates an existing account.
     * 
     * @param tenant_id the tenant ID
     * @param id        the account ID
     * @param dto       the new account data
     * @return the updated account DTO
     * @throws ResourceNotFoundException if account is not found
     */
    @Transactional
    public CompteDto updateCompte(UUID tenant_id, UUID id, CompteDto dto) {
        Compte compte = compte_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));

        compte.setLibelle(dto.getLibelle());
        compte.setNotes(dto.getNotes());
        compte.setUpdated_at(LocalDateTime.now());
        compte.setUpdated_by("system");

        Compte updated = compte_repository.save(compte);

        redis_template.opsForValue().set(CACHE_PREFIX + tenant_id + ":" + updated.getId(), updated.getSolde());
        invalidateCache(tenant_id, updated.getNo_compte());

        return mapToDto(updated);
    }

    /**
     * Deletes an account by ID.
     * 
     * @param tenant_id the tenant ID
     * @param id        the account ID
     * @throws ResourceNotFoundException if account is not found
     */
    @Transactional
    public void deleteById(UUID tenant_id, UUID id) {
        Compte compte = compte_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id.toString()));

        compte_repository.delete(compte);
        invalidateCache(tenant_id, compte.getNo_compte());
        log.info("Account deleted: {} for tenant {}", compte.getNo_compte(), tenant_id);
    }

    /**
     * Maps a CompteDto to a Compte entity.
     * 
     * @param dto the DTO to map
     * @return the mapped entity
     */
    private Compte mapToEntity(CompteDto dto) {
        Compte compte = new Compte();
        compte.setNo_compte(dto.getNo_compte());
        compte.setLibelle(dto.getLibelle());
        compte.setNotes(dto.getNotes());
        compte.setType_compte(dto.getType_compte());
        compte.setClasse(dto.getClasse());
        compte.setActif(dto.getActif() != null ? dto.getActif() : true);
        compte.setSolde(dto.getSolde() != null ? dto.getSolde() : BigDecimal.ZERO);
        return compte;
    }

    /**
     * Maps a Compte entity to a CompteDto.
     * 
     * @param compte the entity to map
     * @return the mapped DTO
     */
    private CompteDto mapToDto(Compte compte) {
        return CompteDto.builder()
                .id(compte.getId())
                .no_compte(compte.getNo_compte())
                .libelle(compte.getLibelle())
                .notes(compte.getNotes())
                .type_compte(compte.getType_compte())
                .classe(compte.getClasse())
                .solde(compte.getSolde())
                .actif(compte.getActif())
                .tenant_id(compte.getTenant() != null ? compte.getTenant().getId() : null)
                .created_at(compte.getCreated_at())
                .updated_at(compte.getUpdated_at())
                .build();
    }

    /**
     * Invalidates cache for a tenant and account.
     * 
     * @param tenant_id the tenant ID
     * @param no_compte the account number
     */
    private void invalidateCache(UUID tenant_id, String no_compte) {
        redis_template.delete(CACHE_ALL_PREFIX + tenant_id);
        redis_template.delete(CACHE_BY_NO_COMPTE_PREFIX + tenant_id + ":" + no_compte);
        log.debug("Cache invalidated for tenant {} and account {}", tenant_id, no_compte);
    }
}