package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.NotificationRepositoryPort;

import com.yowyob.erp.accounting.domain.model.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, UUID>, NotificationRepositoryPort {
    Flux<Notification> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, String userId);
    Flux<Notification> findAllByOrganizationIdAndUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID organizationId, String userId);
}
