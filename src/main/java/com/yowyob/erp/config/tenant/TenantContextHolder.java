package com.yowyob.erp.config.tenant;
import com.yowyob.erp.accounting.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;

class TenantContextHolder {
    private static TenantContextHolder instance = new TenantContextHolder();
    @Autowired
    static TenantRepository tenantRepository;

    public static TenantContextHolder getInstance() {
        return instance;
    }
    
}