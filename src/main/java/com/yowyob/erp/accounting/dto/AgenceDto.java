package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Agence.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenceDto {
    private UUID id;
    private UUID organization_id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String country;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
