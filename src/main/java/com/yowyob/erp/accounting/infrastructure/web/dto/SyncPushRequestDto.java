package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushRequestDto {
    private List<SyncPushOperationDto> operations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncPushOperationDto {
        private String clientMutationId;
        private String entity;
        private String action;
        private String entityId;
        private Map<String, Object> payload;
    }
}
