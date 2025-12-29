package com.yowyob.erp.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Elasticsearch search results.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String id;

    @JsonProperty("tenant_id")
    private String tenantId;

    private String type;
    private String title;
    private String description;
    private Map<String, Object> data;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private Double score;
}
