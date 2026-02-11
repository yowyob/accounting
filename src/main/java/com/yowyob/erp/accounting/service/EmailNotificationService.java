package com.yowyob.erp.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@yowyob.com}")
    private String fromEmail;

    public Mono<Void> sendEmail(String to, String subject, String body) {
        return Mono.fromRunnable(() -> {
            log.info("Sending Email to {}: {}", to, subject);
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Failed to send email to {}", to, e);
                throw e;
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
