package com.yowyob.erp.accounting.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for JournalAudit.
 * Used for exposing audit logs through the API.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournalAuditDto {

    private UUID id;

    @JsonProperty("ecritureId")
    private UUID ecriture_comptable_id;

    private String action;

    @JsonProperty("dateAction")
    private LocalDateTime date_action;

    private String utilisateur;

    private String details;

    @JsonProperty("adresseIp")
    private String adresse_ip;

    @JsonProperty("donneesAvant")
    private String donnees_avant;

    @JsonProperty("donneesApres")
    private String donnees_apres;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;

    @JsonProperty("createdBy")
    private String created_by;

    @JsonProperty("updatedBy")
    private String updated_by;
}
