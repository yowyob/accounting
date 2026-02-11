package com.yowyob.erp.accounting.dto;

import com.yowyob.erp.accounting.entity.BrouillardType;
import com.yowyob.erp.accounting.entity.ModeSaisie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingSettingDto {
    private UUID id;
    private BrouillardType objetType;
    private ModeSaisie modeSaisie;
    private BigDecimal montantSeuil;
    private UUID journalId;
    private String journalCode; // Pour affichage
    private Boolean actif;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
