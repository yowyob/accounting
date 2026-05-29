package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Notification;
import java.util.UUID;
import reactor.core.publisher.Flux;

/**
 * Output port for Notification persistence operations.
 */
public interface NotificationRepositoryPort {
    Flux<Notification> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, String userId);
    Flux<Notification> findAllByOrganizationIdAndUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID organizationId, String userId);
}
