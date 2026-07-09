package com.yowyob.erp.config.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Reverse-proxy (BFF) du Kernel.
 *
 * <p>Le frontend ne doit pas détenir les credentials de la ClientApplication du Kernel
 * ({@code X-Client-Id} / {@code X-Api-Key}). Ces secrets vivent uniquement côté backend
 * (cf. {@code kernel.client.id} / {@code kernel.client.secret}). Le frontend route donc tous
 * ses appels Kernel via {@code /api/kernel/**} ; ce controller les réémet vers le Kernel à
 * l'aide du {@code kernelWebClient}, qui injecte automatiquement {@code X-Client-Id} et
 * {@code X-Api-Key}.</p>
 *
 * <p>Le Bearer de l'utilisateur ainsi que les en-têtes {@code X-Tenant-Id} /
 * {@code X-Organization-Id} sont transmis tels quels : le Kernel reste seul juge de
 * l'authentification/autorisation de l'utilisateur. Le path est ouvert ({@code permitAll}
 * dans {@code SecurityConfig}) puisque le proxy ne re-valide pas le token côté backend.</p>
 */
@RestController
@RequestMapping("/api/kernel")
public class KernelProxyController {

    /** Préfixe d'application strippé avant de relayer au Kernel. */
    private static final String PROXY_PREFIX = "/api/kernel";

    /** En-têtes client relayés vers le Kernel (les autres sont posés par le WebClient ou ignorés). */
    private static final String[] FORWARDED_HEADERS = {
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            "X-Tenant-Id",
            "X-Tenant-ID",
            "X-Organization-Id",
    };

    private final WebClient kernelWebClient;

    /** Base du Kernel (ex. http://localhost:8080), sans le préfixe /api porté par ses endpoints. */
    private final String kernelBaseUrl;

    public KernelProxyController(WebClient kernelWebClient,
                                 @Value("${auth.api.url}") String kernelBaseUrl) {
        this.kernelWebClient = kernelWebClient;
        // Normalise pour éviter un double slash lors de la concaténation.
        this.kernelBaseUrl = kernelBaseUrl.endsWith("/")
                ? kernelBaseUrl.substring(0, kernelBaseUrl.length() - 1)
                : kernelBaseUrl;
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> proxy(ServerWebExchange exchange,
                                              @RequestBody(required = false) byte[] body) {
        URI incoming = exchange.getRequest().getURI();

        // On conserve l'encodage d'origine (rawPath/rawQuery) pour ne pas ré-encoder.
        // Derrière un reverse-proxy à préfixe (Traefik /accounting-api +
        // SERVER_FORWARD_HEADERS_STRATEGY=framework), rawPath réintègre le préfixe
        // (/accounting-api/api/kernel/...). On localise donc /api/kernel par indexOf
        // au lieu de supposer qu'il est en tête, sinon la cible serait malformée.
        String rawPath = incoming.getRawPath();
        int prefixIdx = rawPath.indexOf(PROXY_PREFIX);
        String forwardedPath = prefixIdx >= 0 && rawPath.length() > prefixIdx + PROXY_PREFIX.length()
                ? rawPath.substring(prefixIdx + PROXY_PREFIX.length())
                : "";
        if (!forwardedPath.startsWith("/")) {
            forwardedPath = "/" + forwardedPath;
        }

        String rawQuery = incoming.getRawQuery();
        // Les endpoints du Kernel sont servis sous /api (auth.api.url n'inclut pas /api).
        String target = kernelBaseUrl + "/api" + forwardedPath
                + (rawQuery != null && !rawQuery.isEmpty() ? "?" + rawQuery : "");

        HttpMethod method = exchange.getRequest().getMethod();
        HttpHeaders incomingHeaders = exchange.getRequest().getHeaders();

        WebClient.RequestBodySpec request = kernelWebClient
                .method(method)
                .uri(URI.create(target))
                .headers(headers -> {
                    for (String name : FORWARDED_HEADERS) {
                        String value = incomingHeaders.getFirst(name);
                        if (value != null) {
                            headers.set(name, value);
                        }
                    }
                    // Le client généré envoie parfois X-Tenant-ID ; le Kernel lit X-Tenant-Id.
                    if (!headers.containsKey("X-Tenant-Id")) {
                        String tenantId = firstNonBlank(
                                incomingHeaders.getFirst("X-Tenant-Id"),
                                incomingHeaders.getFirst("X-Tenant-ID"));
                        if (tenantId != null) {
                            headers.set("X-Tenant-Id", tenantId);
                        }
                    }
                });

        WebClient.RequestHeadersSpec<?> spec =
                (body != null && body.length > 0) ? request.bodyValue(body) : request;

        return spec.exchangeToMono(response -> response.toEntity(byte[].class)
                .map(entity -> {
                    ResponseEntity.BodyBuilder builder = ResponseEntity.status(entity.getStatusCode());
                    MediaType contentType = entity.getHeaders().getContentType();
                    if (contentType != null) {
                        builder.contentType(contentType);
                    }
                    return builder.body(entity.getBody());
                }))
                .onErrorResume(WebClientResponseException.class, ex -> Mono.just(
                        ResponseEntity.status(ex.getStatusCode())
                                .contentType(ex.getHeaders().getContentType())
                                .body(ex.getResponseBodyAsByteArray())))
                .onErrorResume(error -> Mono.just(
                        ResponseEntity.status(502)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(("Kernel unreachable: " + error.getMessage()).getBytes())));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }
}
