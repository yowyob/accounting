package com.yowyob.erp.shared.infrastructure.persistence;

import org.springframework.data.domain.Persistable;

public interface SettablePersistable<ID> extends Persistable<ID> {
    void setNotNew();
}
