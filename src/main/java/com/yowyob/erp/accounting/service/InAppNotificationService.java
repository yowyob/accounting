package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Notification;
import com.yowyob.erp.accounting.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {

    private final NotificationRepository repository;
    private final SseNotificationBroker sseBroker;

    public Mono<Notification> createNotification(UUID organizationId, String userId, String title, String message, String type, String referenceId) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(notification)
                .doOnSuccess(n -> {
                    log.info("In-App Notification created for user {}: {}", userId, title);
                    // Push real-time SSE event to all connected clients of this tenant
                    sseBroker.emit(organizationId, n);
                });
    }

    public Flux<Notification> getUnreadNotifications(UUID organizationId, String userId) {
        return repository.findAllByOrganizationIdAndUserIdAndIsReadFalseOrderByCreatedAtDesc(organizationId, userId);
    }

    public Mono<Notification> markAsRead(UUID id) {
        return repository.findById(id)
                .flatMap(n -> {
                    n.setIsRead(true);
                    n.setReadAt(LocalDateTime.now());
                    return repository.save(n);
                });
    }
}
