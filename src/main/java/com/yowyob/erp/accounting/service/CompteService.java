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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing OHADA accounting accounts.
 * Compatible with PostgreSQL + Redis Cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService {

    private final CompteRepository compteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TenantRepository tenantRepository;

    private static final String CACHE_PREFIX = "compte:solde:";
    private static final String CACHE_ALL_PREFIX = "compte:all:";
    private static final String CACHE_BY_NO_COMPTE_PREFIX = "compte:nocompte:";

    /* CREATION */
    @Transactional
    public CompteDto createCompte(CompteDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }
        log.info("Creating account for tenant {} with number {}", tenantId, dto.getNoCompte());

        Compte compte = mapToEntity(dto);
        compte.setTenant(tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString())));
        compte.setCreatedAt(LocalDateTime.now());
        compte.setUpdatedAt(LocalDateTime.now());
        compte.setCreatedBy("system");
        compte.setUpdatedBy("system");
        compte.setSolde(BigDecimal.ZERO);
        compte.setActif(true);

        Compte saved = compteRepository.save(compte);

        // Cache the balance
        redisTemplate.opsForValue().set(CACHE_PREFIX + tenantId + ":" + saved.getId(), saved.getSolde());
        invalidateCache(tenantId, saved.getNoCompte());

        return mapToDto(saved);
    }

    /* READING */
    public List<CompteDto> findAllByTenant(UUID tenantId) {
        String cacheKey = CACHE_ALL_PREFIX + tenantId;
        @SuppressWarnings("unchecked")
        List<CompteDto> comptes = (List<CompteDto>) redisTemplate.opsForValue().get(cacheKey);

        if (comptes == null) {
            comptes = compteRepository.findAllByTenant_Id(tenantId)
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(cacheKey, comptes);
        }
        return comptes;
    }

    public Optional<CompteDto> findById(UUID tenantId, UUID id) {
        Compte compte = compteRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id.toString()));
        String cacheKey = CACHE_PREFIX + tenantId + ":" + id;

        @SuppressWarnings("unchecked")
        BigDecimal cachedSolde = (BigDecimal) redisTemplate.opsForValue().get(cacheKey);
        if (cachedSolde != null) {
            compte.setSolde(cachedSolde);
        } else {
            redisTemplate.opsForValue().set(cacheKey, compte.getSolde());
        }

        return Optional.of(mapToDto(compte));
    }

    public List<CompteDto> findByNoCompte(UUID tenantId, String noCompte) {
        String cacheKey = CACHE_BY_NO_COMPTE_PREFIX + tenantId + ":" + noCompte;
        @SuppressWarnings("unchecked")
        List<CompteDto> cached = (List<CompteDto>) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<CompteDto> comptes = compteRepository.findByTenant_IdAndNoCompte(tenantId, noCompte)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, comptes);
        return comptes;
    }

    /* UPDATE */
    @Transactional
    public CompteDto updateCompte(UUID tenantId, UUID id, CompteDto dto) {
        Compte compte = compteRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id.toString()));

        compte.setLibelle(dto.getLibelle());
        compte.setNotes(dto.getNotes());
        compte.setUpdatedAt(LocalDateTime.now());
        compte.setUpdatedBy("system");

        Compte updated = compteRepository.save(compte);

        redisTemplate.opsForValue().set(CACHE_PREFIX + tenantId + ":" + updated.getId(), updated.getSolde());
        invalidateCache(tenantId, updated.getNoCompte());

        return mapToDto(updated);
    }

    /* DELETION */
    @Transactional
    public void deleteById(UUID tenantId, UUID id) {
        Compte compte = compteRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id.toString()));

        compteRepository.delete(compte);
        invalidateCache(tenantId, compte.getNoCompte());
        log.info("Account deleted: {} for tenant {}", compte.getNoCompte(), tenantId);
    }

    /* MAPPING */
    private Compte mapToEntity(CompteDto dto) {
        Compte compte = new Compte();
        compte.setNoCompte(dto.getNoCompte());
        compte.setLibelle(dto.getLibelle());
        compte.setNotes(dto.getNotes());
        compte.setTypeCompte(dto.getTypeCompte());
        compte.setClasse(dto.getClasse());
        compte.setActif(dto.getActif() != null ? dto.getActif() : true);
        compte.setSolde(dto.getSolde() != null ? dto.getSolde() : BigDecimal.ZERO);
        return compte;
    }

    private CompteDto mapToDto(Compte compte) {
        return CompteDto.builder()
                .id(compte.getId())
                .noCompte(compte.getNoCompte())
                .libelle(compte.getLibelle())
                .notes(compte.getNotes())
                .typeCompte(compte.getTypeCompte())
                .classe(compte.getClasse())
                .solde(compte.getSolde())
                .actif(compte.getActif())
                .tenantId(compte.getTenant() != null ? compte.getTenant().getId() : null) // Assume Tenant has getId()
                .createdAt(compte.getCreatedAt())
                .updatedAt(compte.getUpdatedAt())
                .build();
    }

    /* CACHE UTILS */
    private void invalidateCache(UUID tenantId, String noCompte) {
        redisTemplate.delete(CACHE_ALL_PREFIX + tenantId);
        redisTemplate.delete(CACHE_BY_NO_COMPTE_PREFIX + tenantId + ":" + noCompte);
        log.debug("Cache invalidated for tenant {} and account {}", tenantId, noCompte);
    }
}