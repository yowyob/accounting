package com.yowyob.erp.config.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service utilitaire Redis pour :
 * - la gestion des sessions
 * - le cache applicatif
 * - les soldes de comptes
 * - la vérification de clés
 *
 * Conforme à la charte Yowyob : sécurité, traçabilité, maintenabilité
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sauvegarde une valeur avec TTL
     */
    public void save(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("💾 Valeur sauvegardée dans Redis : {}", key);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la sauvegarde Redis pour la clé {}", key, e);
        }
    }

    /**
     * Récupère une valeur avec type attendu
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération Redis pour la clé {}", key, e);
            return null;
        }
    }

    /**
     * Supprime une clé
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("🗑️ Clé supprimée : {}", key);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression Redis pour {}", key, e);
        }
    }

    /**
     * Vérifie si une clé existe
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification d'existence Redis : {}", key, e);
            return false;
        }
    }

    /**
     * Gestion des soldes de comptes
     */
    public void saveAccountBalance(String tenantId, String accountNumber, Double balance) {
        save(String.format("account:balance:%s:%s", tenantId, accountNumber), balance, Duration.ofMinutes(30));
    }

    public Double getAccountBalance(String tenantId, String accountNumber) {
        return get(String.format("account:balance:%s:%s", tenantId, accountNumber), Double.class);
    }

    /**
     * Gestion des sessions utilisateur
     */
    public void saveUserSession(String sessionId, Object userInfo, Duration ttl) {
        save("session:" + sessionId, userInfo, ttl);
    }

    public <T> T getUserSession(String sessionId, Class<T> type) {
        return get("session:" + sessionId, type);
    }
}
