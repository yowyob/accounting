package com.yowyob.erp.accounting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class InfobipSmsService {

    private final WebClient webClient;

    @Value("${infobip.api.key:}")
    private String apiKey;

    @Value("${infobip.base.url:https://api.infobip.com}")
    private String baseUrl;

    @Value("${infobip.sender:Yowyob}")
    private String sender;

    public InfobipSmsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<Void> sendSms(String phoneNumber, String message) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Infobip API Key not configured. Skipping SMS to {}", phoneNumber);
            return Mono.empty();
        }

        log.info("Sending SMS to {}: {}", phoneNumber, message);

        Map<String, Object> body = Map.of(
            "messages", List.of(
                Map.of(
                    "from", sender,
                    "destinations", List.of(Map.of("to", phoneNumber)),
                    "text", message
                )
            )
        );

        return webClient.post()
                .uri(baseUrl + "/sms/2/text/advanced")
                .header("Authorization", "App " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(response -> log.debug("SMS sent successfully: {}", response.getStatusCode()))
                .doOnError(error -> log.error("Failed to send SMS to {}", phoneNumber, error))
                .then();
    }
}
