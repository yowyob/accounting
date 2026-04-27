package com.yowyob.erp.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Clôture automatique planifiée des périodes comptables expirées.
 *
 * Chaque nuit à 02h00, ce job :
 *   1. Récupère toutes les périodes non clôturées dont la date_fin < aujourd'hui - delai_jours
 *   2. Vérifie l'éligibilité (aucune écriture non validée)
 *   3. Clôture automatiquement si éligible et notifie l'organisation
 *
 * Activable/désactivable via : cloture.auto.enabled=true/false
 * Délai configurable   via : cloture.auto.delai_jours=5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCloturePeriodeService {

    private final ClotureMensuelleService cloture_service;
    private final InAppNotificationService notification_service;
    private final DatabaseClient databaseClient;

    @Value("${cloture.auto.enabled:true}")
    private boolean autoEnabled;

    @Value("${cloture.auto.delai_jours:5}")
    private int delaiJours;

    @Scheduled(cron = "0 0 2 * * *")
    public void cloturerPeriodesExpirees() {
        if (!autoEnabled) {
            log.debug("[AutoCloture] désactivée (cloture.auto.enabled=false)");
            return;
        }

        LocalDate seuil = LocalDate.now().minusDays(delaiJours);
        log.info("[AutoCloture] Démarrage — seuil : {}", seuil);

        databaseClient.sql("""
                SELECT p.id, p.organization_id, p.code
                FROM periodes_comptables p
                WHERE p.cloturee = false
                  AND p.date_fin <= :seuil
                ORDER BY p.date_fin ASC
                """)
            .bind("seuil", seuil)
            .fetch()
            .all()
            .flatMap(row -> traiterPeriode(
                (UUID) row.get("id"),
                (UUID) row.get("organization_id"),
                (String) row.get("code")))
            .subscribe(
                msg -> log.info("[AutoCloture] {}", msg),
                err -> log.error("[AutoCloture] Erreur inattendue : {}", err.getMessage())
            );
    }

    private Mono<String> traiterPeriode(UUID periodeId, UUID orgId, String code) {
        return compterEcrituresNonValidees(periodeId)
            .flatMap(count -> {
                if (count > 0L) {
                    log.warn("[AutoCloture] Période {} ignorée : {} écriture(s) non validée(s)", code, count);
                    return Mono.<String>just("Ignoré : " + code + " (" + count + " écritures non validées)");
                }
                return clotureEtNotifier(periodeId, orgId, code);
            });
    }

    private Mono<Long> compterEcrituresNonValidees(UUID periodeId) {
        return databaseClient.sql("""
                SELECT COUNT(*) AS cnt
                FROM ecritures_comptables
                WHERE periode_id = :periodeId AND validee = false
                """)
            .bind("periodeId", periodeId)
            .map(row -> row.get("cnt", Long.class))
            .one()
            .defaultIfEmpty(0L);
    }

    private Mono<String> clotureEtNotifier(UUID periodeId, UUID orgId, String code) {
        return cloture_service.cloturerPeriode(periodeId, "auto-cloture-system")
            .then(Mono.defer(() -> notification_service.createNotification(
                    orgId,
                    "system",
                    "Clôture automatique",
                    "La période " + code + " a été clôturée automatiquement.",
                    "INFO",
                    periodeId.toString())))
            .thenReturn("Clôturé : " + code)
            .doOnSuccess(msg -> log.info("[AutoCloture] {}", msg))
            .onErrorResume(e -> {
                log.error("[AutoCloture] Échec pour {} : {}", code, e.getMessage());
                return Mono.just("Erreur : " + code + " — " + e.getMessage());
            });
    }
}
