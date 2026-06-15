package com.yowyob.erp.config.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * RÉFÉRENCE — Anti-abus pour un SERVICE EXTERNE appelé en direct (hors des filtres de kernel-core).
 *
 * Sur chaque requête métier entrante, ce filtre demande à kernel-core si le CLIENT APPELANT a le
 * droit d'utiliser ce service ("ACCOUNTING") ET consomme le quota plateforme partagé, via
 * POST /api/client-applications/me/authorize?service=ACCOUNTING en RELAYANT les creds de l'appelant
 * (X-Client-Id / X-Api-Key). kernel-core répond 200 (ok) / 403 (non habilité) / 429 (quota dépassé).
 *
 * Désactivé par défaut (kernel.entitlement.enabled=false) : à activer une fois que les appelants
 * (BFF / autres backends) transmettent leurs creds client. Fail-open en cas d'indisponibilité du
 * kernel pour ne pas couper le service (réglable via kernel.entitlement.fail-open).
 *
 * À copier/adapter dans les autres services externes : changer SERVICE_CODE.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "kernel.entitlement.enabled", havingValue = "true")
public class ServiceEntitlementWebFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEntitlementWebFilter.class);
    private static final String SERVICE_CODE = "ACCOUNTING";   // <-- à personnaliser par service
    private static final String CLIENT_ID_HEADER = "X-Client-Id";
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final WebClient webClient;
    private final String kernelBaseUrl;
    private final boolean failOpen;

    public ServiceEntitlementWebFilter(
            @Qualifier("genericWebClient") WebClient webClient,
            @Value("${auth.api.url}") String kernelBaseUrl,
            @Value("${kernel.entitlement.fail-open:true}") boolean failOpen) {
        this.webClient = webClient;
        this.kernelBaseUrl = kernelBaseUrl.replaceAll("/+$", "");
        this.failOpen = failOpen;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth") || path.startsWith("/api/public")) {
            return chain.filter(exchange);
        }
        HttpHeaders in = exchange.getRequest().getHeaders();
        String clientId = in.getFirst(CLIENT_ID_HEADER);
        String apiKey = in.getFirst(API_KEY_HEADER);
        // Pas de creds client appelant -> on laisse passer (appels utilisateur directs en transition).
        if (clientId == null || clientId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return chain.filter(exchange);
        }
        String tenant = in.getFirst(TENANT_HEADER);

        return webClient.post()
                .uri(kernelBaseUrl + "/api/client-applications/me/authorize?service=" + SERVICE_CODE)
                .header(CLIENT_ID_HEADER, clientId)
                .header(API_KEY_HEADER, apiKey)
                .headers(h -> { if (tenant != null) h.set(TENANT_HEADER, tenant); })
                .exchangeToMono(resp -> {
                    HttpStatus status = HttpStatus.resolve(resp.statusCode().value());
                    if (status != null && status.is2xxSuccessful()) {
                        return resp.releaseBody().then(chain.filter(exchange));
                    }
                    if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
                        exchange.getResponse().setStatusCode(status);
                        String retryAfter = resp.headers().asHttpHeaders().getFirst("Retry-After");
                        if (retryAfter != null) {
                            exchange.getResponse().getHeaders().set("Retry-After", retryAfter);
                        }
                        return resp.releaseBody().then(exchange.getResponse().setComplete());
                    }
                    // Réponse inattendue du kernel -> politique fail-open/closed.
                    return resp.releaseBody().then(Mono.defer(() -> failOpenOrDeny(exchange, chain,
                            "unexpected status " + resp.statusCode())));
                })
                .onErrorResume(error -> failOpenOrDeny(exchange, chain, error.toString()));
    }

    private Mono<Void> failOpenOrDeny(ServerWebExchange exchange, WebFilterChain chain, String reason) {
        LOGGER.warn("Entitlement check unavailable ({}). fail-open={}", reason, failOpen);
        if (failOpen) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }
}
