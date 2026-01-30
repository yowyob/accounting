package com.yowyob.erp.common.persistence;

import org.springframework.data.domain.Persistable;

public interface SettablePersistable<ID> extends Persistable<ID> {
    void setNotNew();
}
