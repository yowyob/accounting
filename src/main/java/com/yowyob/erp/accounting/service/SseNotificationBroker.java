package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SSE streams per tenant organization.
 * When a new notification is created, it is pushed to all connected clients of that tenant.
 */
@Service
@Slf4j
public class SseNotificationBroker {

    // One Sink per tenant. Multicast = multiple subscribers (multiple browser tabs)
    private final Map<UUID, Sinks.Many<ServerSentEvent<Notification>>> sinks = new ConcurrentHashMap<>();

    /**
     * Returns an SSE stream for the given tenant.
     * Creates the sink if not already present.
     */
    public Flux<ServerSentEvent<Notification>> streamForTenant(UUID tenantId) {
        Sinks.Many<ServerSentEvent<Notification>> sink = sinks.computeIfAbsent(tenantId,
                id -> Sinks.many().multicast().onBackpressureBuffer(64, false));

        return sink.asFlux()
                .doOnCancel(() -> log.debug("SSE client disconnected for tenant {}", tenantId))
                .doOnTerminate(() -> log.debug("SSE stream terminated for tenant {}", tenantId));
    }

    /**
     * Emits a notification to all connected clients of the given tenant.
     */
    public void emit(UUID tenantId, Notification notification) {
        Sinks.Many<ServerSentEvent<Notification>> sink = sinks.get(tenantId);
        if (sink != null) {
            ServerSentEvent<Notification> event = ServerSentEvent.<Notification>builder()
                    .id(notification.getId().toString())
                    .event("notification")
                    .data(notification)
                    .build();

            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.warn("Failed to emit SSE notification to tenant {}: {}", tenantId, result);
            } else {
                log.debug("SSE notification emitted to tenant {}: {}", tenantId, notification.getTitle());
            }
        }
    }
}
