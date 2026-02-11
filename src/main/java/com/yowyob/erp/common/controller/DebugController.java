package com.yowyob.erp.common.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.erp.accounting.service.SynchronizationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.OrganizationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🎯 DebugController
 *
 * Contrôleur utilitaire de test et de diagnostic interne :
 * - Vérifie la connectivité Redis et Kafka
 * - Déclenche des synchronisations manuelles
 * - Expose le contexte Organization courant
 *
 * À utiliser uniquement en environnement de développement, recette ou QA.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final KafkaMessageService kafkaMessageService;
    private final RedisService redisService;
    private final SynchronizationService synchronizationService;

    /**
     * 🧩 Teste l’envoi d’un message Kafka sur le topic comptable
     */
    @PostMapping("/kafka/test")
    public ApiResponseWrapper<String> testKafka(@RequestBody Map<String, Object> payload) {
        UUID organizationId = OrganizationContext.getCurrentOrganizationId();
        log.info("🛰️ Envoi d’un message Kafka de test pour le organization {}", organizationId);

        kafkaMessageService.sendAccountingEvent(payload, organizationId, "DEBUG_TEST_EVENT");
        return ApiResponseWrapper.success("✅ Message Kafka de test envoyé avec succès");
    }

    /**
     * 💾 Enregistre des données de test dans Redis
     */
    @PostMapping("/redis/test")
    public ApiResponseWrapper<String> testRedis(@RequestBody Map<String, Object> data) {
        UUID organizationId = OrganizationContext.getCurrentOrganization();
        String key = String.format("debug:test:%s", organizationId);
        redisService.save(key, data, Duration.ofMinutes(5));

        log.info("💾 Données de test enregistrées dans Redis pour le organization {}", organizationId);
        return ApiResponseWrapper.success("✅ Données sauvegardées en Redis");
    }

    /**
     * 🔍 Récupère les données précédemment enregistrées dans Redis
     */
    @GetMapping("/redis/test")
    public ApiResponseWrapper<Object> getRedisTest() {
        UUID organizationId = OrganizationContext .getCurrentOrganization();
        String key = String.format("debug:test:%s", organizationId);

        Object data = redisService.get(key, Object.class);
        log.info("🔍 Lecture Redis : clé={} valeur={}", key, data);
        return ApiResponseWrapper.success(data != null ? data : "⚠️ Aucune donnée trouvée pour cette clé");
    }

    /**
     * 🔁 Déclenche manuellement une synchronisation des écritures comptables
     */
    @PostMapping("/sync/test")
    public ApiResponseWrapper<String> testSync() {
        UUID organizationId = OrganizationContext.getCurrentOrganization();
        log.info("🔁 Déclenchement manuel de la synchronisation pour le organization {}", organizationId);

        synchronizationService.checkAndSync(organizationId);
        return ApiResponseWrapper.success("✅ Synchronisation déclenchée avec succès");
    }

    /**
     * 🧠 Affiche les informations du contexte Organization courant
     */
    @GetMapping("/organization/info")
    public ApiResponseWrapper<Map<String, Object>> getOrganizationInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("organizationId", OrganizationContext.getCurrentOrganization());
        info.put("thread", Thread.currentThread().getName());
        info.put("timestamp", System.currentTimeMillis());

        log.debug("🧠 Contexte organization : {}", info);
        return ApiResponseWrapper.success(info);
    }

    /**
     * 🧹 Vide une clé Redis spécifique (utile pour le debug)
     */
    @DeleteMapping("/redis/clear/{key}")
    public ApiResponseWrapper<String> clearRedisKey(@PathVariable String key) {
        redisService.delete(key);
        log.info("🧹 Clé Redis supprimée manuellement : {}", key);
        return ApiResponseWrapper.success("✅ Clé supprimée : " + key);
    }
}
