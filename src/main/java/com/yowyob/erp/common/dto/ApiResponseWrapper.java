package com.yowyob.erp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 🌍 ApiResponseWrapper
 *
 * Structure normalisée de réponse API pour l'ensemble des modules Yowyob ERP.
 * 
 * Avantages :
 * ✅ Format uniforme (succès/erreur)
 * ✅ Traçabilité complète (timestamp, traceId)
 * ✅ Utilisable dans tous les microservices REST & Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseWrapper<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Statut logique du traitement */
    private boolean success;

    /** Code HTTP ou interne */
    @Builder.Default
    private int statusCode = 200;

    /** Message explicatif */
    @Builder.Default
    private String message = "Opération réussie";

    /** Données de retour */
    private T data;

    /** Heure exacte du traitement */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Identifiant de traçabilité pour corrélation inter-services */
    @Builder.Default
    private String traceId = UUID.randomUUID().toString();

    /** (Optionnel) chemin de la requête */
    private String path;

    // ✅ Méthodes statiques utilitaires
    public static <T> ApiResponseWrapper<T> success(T data) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .statusCode(200)
                .message("Opération réussie")
                .data(data)
                .build();
    }

    public static <T> ApiResponseWrapper<T> success(T data, String message) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponseWrapper<T> error(String message) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .build();
    }

    public static <T> ApiResponseWrapper<T> error(String message, int code) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .statusCode(code)
                .message(message)
                .build();
    }
}
