package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.repository.CompteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de gestion des comptes comptables OHADA.
 * Compatible PostgreSQL + JPA + Redis Cache.
 */
@Service
public class CompteService {

    @Autowired
    private CompteRepository compteRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "compte:solde:";
    private static final String CACHE_ALL_PREFIX = "compte:all:";
    private static final String CACHE_BY_NO_COMPTE_PREFIX = "compte:nocompte:";

    /* -------------------------------------------------------------------------- */
    /*                                  CREATION                                  */
    /* -------------------------------------------------------------------------- */

    @Transactional
    public Compte createCompte(Compte compte) {
        compte.setCreatedAt(LocalDateTime.now());
        compte.setUpdatedAt(LocalDateTime.now());
        compte.setCreatedBy("system");
        compte.setUpdatedBy("system");
        compte.setSoldes(BigDecimal.ZERO);

        Compte saved = compteRepository.save(compte);

        // Cache du solde
        redisTemplate.opsForValue().set(CACHE_PREFIX + saved.getTenantId() + ":" + saved.getId(), saved.getSoldes());
        invalidateCache(saved.getTenantId(), saved.getNoCompte());

        return saved;
    }

    /* -------------------------------------------------------------------------- */
    /*                                   LECTURE                                  */
    /* -------------------------------------------------------------------------- */

    public List<Compte> findAllByTenantId(UUID tenantId) {
        String cacheKey = CACHE_ALL_PREFIX + tenantId;
        List<Compte> comptes = (List<Compte>) redisTemplate.opsForValue().get(cacheKey);

        if (comptes == null) {
            comptes = compteRepository.findAllByTenantId(tenantId);
            if (!comptes.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, comptes);
            }
        }
        return comptes != null ? comptes : List.of();
    }

    public Optional<Compte> findById(Long id, UUID tenantId) {
        String cacheKey = CACHE_PREFIX + tenantId + ":" + id;
        Object cachedSolde = redisTemplate.opsForValue().get(cacheKey);

        Optional<Compte> compteOpt = compteRepository.findById(id);
        if (compteOpt.isEmpty() || !tenantId.equals(compteOpt.get().getTenantId())) {
            return Optional.empty();
        }

        Compte compte = compteOpt.get();
        if (cachedSolde instanceof BigDecimal solde) {
            compte.setSoldes(solde);
        } else {
            redisTemplate.opsForValue().set(cacheKey, compte.getSoldes());
        }

        return Optional.of(compte);
    }

    public Optional<Compte> findByNoCompte(UUID tenantId, String noCompte) {
        String cacheKey = CACHE_BY_NO_COMPTE_PREFIX + tenantId + ":" + noCompte;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached instanceof Compte compte) {
            return Optional.of(compte);
        }

        Optional<Compte> compteOpt = compteRepository.findByTenantIdAndNoCompte(tenantId, noCompte);
        compteOpt.ifPresent(c -> redisTemplate.opsForValue().set(cacheKey, c));
        return compteOpt;
    }

    /* -------------------------------------------------------------------------- */
    /*                                   MISE À JOUR                              */
    /* -------------------------------------------------------------------------- */

    @Transactional
    public Compte updateCompte(Compte compte) {
        compte.setUpdatedAt(LocalDateTime.now());
        compte.setUpdatedBy("system");
        Compte updated = compteRepository.save(compte);

        redisTemplate.opsForValue().set(CACHE_PREFIX + updated.getTenantId() + ":" + updated.getId(), updated.getSoldes());
        invalidateCache(updated.getTenantId(), updated.getNoCompte());

        return updated;
    }

    /* -------------------------------------------------------------------------- */
    /*                                   SUPPRESSION                              */
    /* -------------------------------------------------------------------------- */

    @Transactional
    public void deleteById(Long id, UUID tenantId) {
        Optional<Compte> compteOpt = compteRepository.findById(id);
        compteOpt.ifPresent(compte -> {
            if (tenantId.equals(compte.getTenantId())) {
                invalidateCache(tenantId, compte.getNoCompte());
                compteRepository.delete(compte);
            }
        });
    }

    /* -------------------------------------------------------------------------- */
    /*                                   UTILITAIRE                               */
    /* -------------------------------------------------------------------------- */

    private void invalidateCache(UUID tenantId, String noCompte) {
        redisTemplate.delete(CACHE_ALL_PREFIX + tenantId);
        redisTemplate.delete(CACHE_BY_NO_COMPTE_PREFIX + tenantId + ":" + noCompte);
    }
}
