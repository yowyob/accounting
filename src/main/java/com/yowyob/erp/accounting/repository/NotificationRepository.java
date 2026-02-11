package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {
    Flux<Notification> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, String userId);
    Flux<Notification> findAllByOrganizationIdAndUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID organizationId, String userId);
}
