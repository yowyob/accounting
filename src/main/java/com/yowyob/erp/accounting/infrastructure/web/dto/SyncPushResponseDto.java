package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushResponseDto {
    @Builder.Default
    private List<SyncPushItemResultDto> results = new ArrayList<>();
    private int synced;
    private int failed;
    private int alreadyProcessed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncPushItemResultDto {
        private String clientMutationId;
        private String entityId;
        private String status;
        private String message;
        private Object data;
    }
}
