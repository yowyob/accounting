package com.yowyob.erp.config.organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;

class OrganizationContextHolder {
    private static OrganizationContextHolder instance = new OrganizationContextHolder();
    @Autowired
    static OrganizationRepository organizationRepository;

    public static OrganizationContextHolder getInstance() {
        return instance;
    }
    
}