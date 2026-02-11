package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.AccountingSettingDto;
import com.yowyob.erp.accounting.entity.AccountingSetting;
import com.yowyob.erp.accounting.entity.BrouillardType;
import com.yowyob.erp.accounting.entity.ModeSaisie;
import com.yowyob.erp.accounting.repository.AccountingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingSettingService {

    private final AccountingSettingRepository repository;

    public Mono<AccountingSetting> getSetting(UUID organizationId, BrouillardType type, UUID journalId) {
        if (journalId != null) {
            return repository.findByTenantIdAndObjetTypeAndJournalId(organizationId, type, journalId)
                    .switchIfEmpty(repository.findByTenantIdAndObjetTypeAndJournalIdIsNull(organizationId, type));
        }
        return repository.findByTenantIdAndObjetTypeAndJournalIdIsNull(organizationId, type);
    }

    public Mono<Boolean> shouldUseBrouillard(UUID organizationId, BrouillardType type, BigDecimal amount, UUID journalId) {
        return getSetting(organizationId, type, journalId)
                .map(setting -> {
                    if (setting.getModeSaisie() == ModeSaisie.AUTOMATIQUE) {
                        return false;
                    }
                    if (setting.getMontantSeuil() != null && amount != null) {
                        return amount.compareTo(setting.getMontantSeuil()) > 0;
                    }
                    return true;
                })
                .defaultIfEmpty(true); // Default to semi-automatic (draft) if no setting found
    }

    public Flux<AccountingSetting> getAllSettings(UUID organizationId) {
        return repository.findAllByTenantId(organizationId);
    }
    
    public Mono<AccountingSetting> updateSetting(UUID organizationId, AccountingSettingDto dto) {
        if (dto.getObjetType() == null) {
            return Mono.error(new IllegalArgumentException("Object type is required"));
        }

        Mono<AccountingSetting> existingSetting;
        if (dto.getJournalId() != null) {
            existingSetting = repository.findByTenantIdAndObjetTypeAndJournalId(organizationId, dto.getObjetType(), dto.getJournalId());
        } else {
            existingSetting = repository.findByTenantIdAndObjetTypeAndJournalIdIsNull(organizationId, dto.getObjetType());
        }

        return existingSetting
                .switchIfEmpty(Mono.defer(() -> {
                    AccountingSetting newSetting = AccountingSetting.builder()
                        .id(UUID.randomUUID())
                        .organizationId(organizationId)
                        .objetType(dto.getObjetType())
                        .journalId(dto.getJournalId())
                        .createdAt(LocalDateTime.now())
                        .build();
                    return Mono.just(newSetting);
                }))
                .flatMap(setting -> {
                    setting.setModeSaisie(dto.getModeSaisie());
                    setting.setMontantSeuil(dto.getMontantSeuil());
                    setting.setActif(dto.getActif() != null ? dto.getActif() : true);
                    setting.setDescription(dto.getDescription());
                    setting.setUpdatedAt(LocalDateTime.now());
                    return repository.save(setting);
                });
    }
}
