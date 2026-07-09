package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCgRequestDto {
    private UUID periodeId;
    private UUID exerciceId;
    @Builder.Default
    private Boolean force = false;
}
