package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.CoutProduit;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.CoutProduitRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.CoutProduitDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoutProduitService {

    private final CoutProduitRepository repository;

    @Transactional
    public Mono<CoutProduitDto> create(CoutProduitDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return assertUniqueness(orgId, dto, null)
                    .then(Mono.defer(() -> {
                        LocalDateTime now = LocalDateTime.now();
                        CoutProduit entity = CoutProduit.builder()
                            .id(UUID.randomUUID())
                            .organizationId(orgId)
                            .produitCode(dto.getProduitCode().trim())
                            .produitLibelle(dto.getProduitLibelle().trim())
                            .coutAchat(dto.getCoutAchat())
                            .coutProduction(dto.getCoutProduction())
                            .coutRevient(dto.getCoutRevient())
                            .methodeStock(dto.getMethodeStock())
                            .periodeId(dto.getPeriodeId())
                            .createdAt(now)
                            .updatedAt(now)
                            .createdBy(user)
                            .updatedBy(user)
                            .build();
                        return repository.save(entity).map(this::toDto);
                    }));
            });
    }

    @Transactional
    public Mono<CoutProduitDto> update(UUID id, CoutProduitDto dto) {
        validateBusinessRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(e -> orgId.equals(e.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("CoutProduit", id.toString())))
                    .flatMap(existing -> assertUniqueness(orgId, dto, id)
                        .then(Mono.defer(() -> {
                            existing.setProduitCode(dto.getProduitCode().trim());
                            existing.setProduitLibelle(dto.getProduitLibelle().trim());
                            existing.setCoutAchat(dto.getCoutAchat());
                            existing.setCoutProduction(dto.getCoutProduction());
                            existing.setCoutRevient(dto.getCoutRevient());
                            existing.setMethodeStock(dto.getMethodeStock());
                            existing.setPeriodeId(dto.getPeriodeId());
                            existing.setUpdatedAt(LocalDateTime.now());
                            existing.setUpdatedBy(user);
                            existing.setNotNew();
                            return repository.save(existing).map(this::toDto);
                        })));
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CoutProduit", id.toString())))
                .flatMap(repository::delete));
    }

    public Mono<CoutProduitDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(e -> orgId.equals(e.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("CoutProduit", id.toString())))
                .map(this::toDto));
    }

    public Flux<CoutProduitDto> getAll(UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> periodeId != null
                ? repository.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                : repository.findByOrganizationId(orgId))
            .map(this::toDto);
    }

    private void validateBusinessRules(CoutProduitDto dto) {
        List<String> methodes = List.of("CUMP", "FIFO", "LIFO");
        if (dto.getMethodeStock() != null && !methodes.contains(dto.getMethodeStock())) {
            throw new BusinessException("Méthode de stock invalide : " + dto.getMethodeStock());
        }
        if (dto.getCoutAchat() != null && dto.getCoutAchat().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le coût d'achat ne peut pas être négatif.");
        }
        if (dto.getCoutProduction() != null && dto.getCoutProduction().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le coût de production ne peut pas être négatif.");
        }
        if (dto.getCoutRevient() != null && dto.getCoutRevient().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le coût de revient ne peut pas être négatif.");
        }
    }

    private Mono<Void> assertUniqueness(UUID orgId, CoutProduitDto dto, UUID excludeId) {
        return repository
            .findByOrganizationIdAndProduitCodeIgnoreCaseAndPeriodeId(
                orgId, dto.getProduitCode().trim(), dto.getPeriodeId())
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .hasElement()
            .flatMap(conflict -> conflict
                ? Mono.error(new BusinessException(
                    "Un coût produit existe déjà pour ce code et cette période."))
                : Mono.empty());
    }

    private CoutProduitDto toDto(CoutProduit entity) {
        return CoutProduitDto.builder()
            .id(entity.getId())
            .produitCode(entity.getProduitCode())
            .produitLibelle(entity.getProduitLibelle())
            .coutAchat(entity.getCoutAchat())
            .coutProduction(entity.getCoutProduction())
            .coutRevient(entity.getCoutRevient())
            .methodeStock(entity.getMethodeStock())
            .periodeId(entity.getPeriodeId())
            .build();
    }
}
