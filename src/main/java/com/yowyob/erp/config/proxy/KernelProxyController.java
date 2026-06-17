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
        String rawPath = incoming.getRawPath();
        String forwardedPath = rawPath.length() > PROXY_PREFIX.length()
                ? rawPath.substring(PROXY_PREFIX.length())
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
                }));
    }
}
