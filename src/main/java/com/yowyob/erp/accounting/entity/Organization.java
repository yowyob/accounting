package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.common.persistence.SettablePersistable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an Organization for R2DBC.
 */
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization implements SettablePersistable<UUID> {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column("code")
    private String code;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("address")
    private String address;

    @Column("tax_id")
    private String tax_id;

    @Column("email")
    private String email;

    @Column("telephone")
    private String telephone;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }

    public Organization(UUID id) {
        this.id = id;
    }
}
