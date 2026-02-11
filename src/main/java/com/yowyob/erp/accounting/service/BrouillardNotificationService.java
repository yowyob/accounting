package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.BrouillardComptable;
import com.yowyob.erp.config.auth.AuthService;
import com.yowyob.erp.config.auth.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to notify accountants about new draft entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrouillardNotificationService {

    private final InAppNotificationService inAppService;
    private final EmailNotificationService emailService;
    private final InfobipSmsService smsService;
    private final AuthService authService;

    public Mono<Void> notifyNewBrouillard(BrouillardComptable brouillard) {
        log.info("Notifying about new draft: {}", brouillard.getId());

        String title = "Nouveau brouillard comptable: " + brouillard.getType();
        String message = String.format("Un nouveau brouillard (%s) pour un montant de %s %s est en attente de validation.",
                brouillard.getLibelle(), brouillard.getMontantTotal(), brouillard.getDevise());

        // Notify both ADMIN and ACCOUNTANT roles
        return authService.getOrganizationMembers(brouillard.getOrganizationId())
                .filter(member -> member.isActive() && 
                        ("ADMIN".equalsIgnoreCase(member.getRoleName()) || "ACCOUNTANT".equalsIgnoreCase(member.getRoleName())))
                .collectList()
                .flatMap(members -> {
                    if (members.isEmpty()) {
                        log.warn("No admins or accountants found for tenant {}", brouillard.getOrganizationId());
                        return Mono.empty();
                    }

                    List<Mono<Void>> notificationTasks = new ArrayList<>();

                    for (com.yowyob.erp.config.auth.OrganizationMember member : members) {
                        // 1. In-App
                        notificationTasks.add(inAppService.createNotification(
                                brouillard.getOrganizationId(),
                                member.getUserId().toString(),
                                title,
                                message,
                                "BROUILLARD",
                                brouillard.getId().toString()
                        ).then());

                        // 2. Email
                        if (member.getUserEmail() != null && !member.getUserEmail().isEmpty()) {
                            notificationTasks.add(emailService.sendEmail(member.getUserEmail(), title, message));
                        }

                        // 3. SMS (Infobip) - We need a phone number. 
                        // Currently OrganizationMember doesn't have it in core-docs.json.
                        // We will log a warning or skip if not available.
                        log.debug("SMS notification skipped for {} as phone number is not in member info", member.getUserEmail());
                    }

                    return Mono.when(notificationTasks);
                });
    }
}
