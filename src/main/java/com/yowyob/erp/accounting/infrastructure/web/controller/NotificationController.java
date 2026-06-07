package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.domain.model.Notification;
import com.yowyob.erp.accounting.infrastructure.notification.InAppNotificationService;
import com.yowyob.erp.accounting.infrastructure.notification.SseNotificationBroker;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User Notifications Management")
public class NotificationController {

    private final InAppNotificationService notificationService;
    private final SseNotificationBroker sseBroker;

    /**
     * SSE stream — frontend connects here to receive real-time push notifications.
     * EventSource cannot send headers: token + tenantId are passed as query params.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream for real-time notifications (connect once per session)")
    public Flux<ServerSentEvent<Notification>> stream(
            @RequestParam(name = "tenantId", required = false) UUID tenantIdParam) {

        Mono<UUID> tenantMono = tenantIdParam != null
                ? Mono.just(tenantIdParam)
                : ReactiveOrganizationContext.getOrganizationId()
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "tenantId is required")));

        return tenantMono.flatMapMany(tenantId -> {
            Flux<ServerSentEvent<Notification>> notifications = sseBroker.streamForTenant(tenantId);

            Flux<ServerSentEvent<Notification>> heartbeat = Flux.interval(Duration.ZERO, Duration.ofSeconds(30))
                    .map(tick -> ServerSentEvent.<Notification>builder()
                            .comment("heartbeat")
                            .build());

            return Flux.merge(notifications, heartbeat)
                    .startWith(ServerSentEvent.<Notification>builder()
                            .comment("connected")
                            .build());
        });
    }

    @GetMapping("/unread")
    @Operation(summary = "Get current user's unread notifications")
    public Mono<ResponseEntity<ApiResponseWrapper<List<Notification>>>> getUnread(
            @AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return Mono.just(ResponseEntity.status(401)
                    .<ApiResponseWrapper<List<Notification>>>build());
        }

        String userId = jwt.getClaimAsString("userId");
        if (userId == null)
            userId = jwt.getSubject();

        String finalUserId = userId;
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organizationId -> notificationService.getUnreadNotifications(organizationId, finalUserId)
                        .collectList())
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public Mono<ResponseEntity<ApiResponseWrapper<Notification>>> markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id)
                .map(n -> ResponseEntity.ok(ApiResponseWrapper.success(n, "Notification marked as read")));
    }
}
