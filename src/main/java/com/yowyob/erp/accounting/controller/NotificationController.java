package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.entity.Notification;
import com.yowyob.erp.accounting.service.InAppNotificationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User Notifications Management")
public class NotificationController {

    private final InAppNotificationService notificationService;

    @GetMapping("/unread")
    @Operation(summary = "Get current user's unread notifications")
    public Mono<ResponseEntity<ApiResponseWrapper<List<Notification>>>> getUnread(
            @AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return Mono.just(ResponseEntity.status(401)
                    .<ApiResponseWrapper<List<Notification>>>build());
        }

        String userId = jwt.getClaimAsString("userId"); // Extract from JWT
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
