package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ConfigurationAnalytique;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ConfigurationAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.ConfigurationAnalytiqueDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationAnalytiqueService {

    private static final List<String> METHODES_STOCK_VALIDES = List.of("CUMP", "FIFO", "LIFO");

    private final ConfigurationAnalytiqueRepository repository;

    public Mono<ConfigurationAnalytiqueDto> get() {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findByOrganizationId(orgId)
                .map(this::toDto)
                .defaultIfEmpty(defaultDto()));
    }

    @Transactional
    public Mono<ConfigurationAnalytiqueDto> save(ConfigurationAnalytiqueDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                LocalDateTime now = LocalDateTime.now();
                return repository.findByOrganizationId(orgId)
                    .flatMap(existing -> {
                        applyDto(existing, dto);
                        existing.setUpdatedAt(now);
                        existing.setUpdatedBy(user);
                        existing.setNotNew();
                        return repository.save(existing);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        ConfigurationAnalytique entity = ConfigurationAnalytique.builder()
                            .id(UUID.randomUUID())
                            .organizationId(orgId)
                            .createdAt(now)
                            .updatedAt(now)
                            .createdBy(user)
                            .updatedBy(user)
                            .build();
                        applyDto(entity, dto);
                        return repository.save(entity);
                    }))
                    .map(this::toDto);
            });
    }

    public Mono<Boolean> isImportCgActive() {
        return get().map(dto -> Boolean.TRUE.equals(dto.getImportComptabiliteGeneraleActive()));
    }

    private void validateBusinessRules(ConfigurationAnalytiqueDto dto) {
        if (dto.getMethodeValorisationStocks() != null
            && !METHODES_STOCK_VALIDES.contains(dto.getMethodeValorisationStocks())) {
            throw new BusinessException("Méthode de valorisation invalide : " + dto.getMethodeValorisationStocks());
        }
        if (dto.getPrecision() != null && (dto.getPrecision() < 0 || dto.getPrecision() > 4)) {
            throw new BusinessException("La précision doit être comprise entre 0 et 4.");
        }
        if (dto.getJoursGraceCloture() != null
            && (dto.getJoursGraceCloture() < 0 || dto.getJoursGraceCloture() > 30)) {
            throw new BusinessException("Les jours de grâce doivent être compris entre 0 et 30.");
        }
    }

    private ConfigurationAnalytiqueDto defaultDto() {
        return ConfigurationAnalytiqueDto.builder().build();
    }

    private void applyDto(ConfigurationAnalytique entity, ConfigurationAnalytiqueDto dto) {
        entity.setDevise(dto.getDevise() != null ? dto.getDevise().trim() : "FCFA");
        entity.setPrecision(dto.getPrecision() != null ? dto.getPrecision() : 0);
        entity.setSeparateurMilliers(dto.getSeparateurMilliers() != null ? dto.getSeparateurMilliers() : " ");
        entity.setBloquerApresClotureCg(
            dto.getBloquerApresClotureCg() != null ? dto.getBloquerApresClotureCg() : true);
        entity.setJoursGraceCloture(dto.getJoursGraceCloture() != null ? dto.getJoursGraceCloture() : 5);
        entity.setAutoriserSaisieRetroactive(
            dto.getAutoriserSaisieRetroactive() != null ? dto.getAutoriserSaisieRetroactive() : false);
        entity.setMethodeValorisationStocks(
            dto.getMethodeValorisationStocks() != null ? dto.getMethodeValorisationStocks() : "CUMP");
        entity.setImportComptabiliteGeneraleActive(
            dto.getImportComptabiliteGeneraleActive() != null ? dto.getImportComptabiliteGeneraleActive() : false);
    }

    private ConfigurationAnalytiqueDto toDto(ConfigurationAnalytique entity) {
        return ConfigurationAnalytiqueDto.builder()
            .devise(entity.getDevise())
            .precision(entity.getPrecision())
            .separateurMilliers(entity.getSeparateurMilliers())
            .bloquerApresClotureCg(entity.getBloquerApresClotureCg())
            .joursGraceCloture(entity.getJoursGraceCloture())
            .autoriserSaisieRetroactive(entity.getAutoriserSaisieRetroactive())
            .methodeValorisationStocks(entity.getMethodeValorisationStocks())
            .importComptabiliteGeneraleActive(entity.getImportComptabiliteGeneraleActive())
            .build();
    }
}
