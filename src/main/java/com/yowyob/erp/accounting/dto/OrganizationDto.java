package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Organization.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    private UUID id;
    private String name;
    private String description;
    private String address;
    private String tax_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
