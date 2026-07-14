package com.yowyob.erp.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Teste la logique de cache/repli de {@link LegalDocumentService} sans reseau : le WebClient du
 * kernel est stube par une {@link ExchangeFunction} programmable qui compte les appels.
 */
class LegalDocumentServiceTest {

    private static final String PATH = "/api/settings/legal-documents";

    private final AtomicInteger calls = new AtomicInteger();
    private volatile ClientResponse nextResponse;
    private volatile RuntimeException nextError;

    private WebClient stubClient() {
        ExchangeFunction fn = request -> {
            calls.incrementAndGet();
            if (nextError != null) {
                return Mono.error(nextError);
            }
            return Mono.just(nextResponse);
        };
        return WebClient.builder().exchangeFunction(fn).build();
    }

    private void respondOk(String slug, String content) {
        nextError = null;
        nextResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"success\":true,\"data\":{"
                        + "\"slug\":\"" + slug + "\",\"locale\":\"fr-en\",\"title\":\"T\","
                        + "\"version\":\"v1\",\"content\":\"" + content + "\","
                        + "\"updatedAt\":\"2026-07-14T00:00:00Z\"}}")
                .build();
    }

    @Test
    void fetchesFromKernelAndMapsData() {
        respondOk("terms", "Texte CGU");
        LegalDocumentService service = new LegalDocumentService(stubClient(), PATH, 600);

        LegalDocumentDto dto = service.get("terms").block();

        assertThat(dto).isNotNull();
        assertThat(dto.slug()).isEqualTo("terms");
        assertThat(dto.content()).isEqualTo("Texte CGU");
        assertThat(dto.locale()).isEqualTo("fr-en");
    }

    @Test
    void cachesWithinTtlSoKernelIsHitOnce() {
        respondOk("terms", "A");
        LegalDocumentService service = new LegalDocumentService(stubClient(), PATH, 600);

        service.get("terms").block();
        LegalDocumentDto second = service.get("terms").block();

        assertThat(second.content()).isEqualTo("A");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void servesStaleCacheWhenKernelBecomesUnreachable() {
        respondOk("terms", "cached");
        // TTL 0 => l'entree est immediatement perimee : le 2e appel retente le kernel.
        LegalDocumentService service = new LegalDocumentService(stubClient(), PATH, 0);

        LegalDocumentDto first = service.get("terms").block();
        assertThat(first.content()).isEqualTo("cached");

        nextError = new RuntimeException("kernel down");
        LegalDocumentDto second = service.get("terms").block();

        assertThat(second).isNotNull();
        assertThat(second.content()).isEqualTo("cached");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void propagatesErrorWhenKernelFailsAndNothingCached() {
        LegalDocumentService service = new LegalDocumentService(stubClient(), PATH, 600);
        nextError = new RuntimeException("kernel down");

        assertThatThrownBy(() -> service.get("terms").block())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void mapsKernel404ToNotFound() {
        LegalDocumentService service = new LegalDocumentService(stubClient(), PATH, 600);
        nextError = null;
        nextResponse = ClientResponse.create(HttpStatus.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"success\":false,\"errorCode\":\"LEGAL_DOCUMENT_NOT_FOUND\"}")
                .build();

        assertThatThrownBy(() -> service.get("unknown").block())
                .isInstanceOf(LegalDocumentNotFoundException.class);
    }
}
