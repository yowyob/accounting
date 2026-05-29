package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.OrganizationDto;
import com.yowyob.erp.accounting.domain.port.in.OrganizationUseCase;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.accounting.infrastructure.web.controller.OrganizationController;
import com.yowyob.erp.config.auth.AuthService;
import com.yowyob.erp.config.auth.SecurityConfig;
import com.yowyob.erp.config.auth.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = OrganizationController.class)
@org.springframework.context.annotation.Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class OrganizationControllerTest {

        @Autowired
        private WebTestClient webTestClient;

        @MockitoBean
        private OrganizationUseCase organizationService;

        @MockitoBean
        private AuthService authService;

        @Test
        @WithMockUser
        public void testCreateOrganization() {
                OrganizationDto dto = OrganizationDto.builder()
                                .name("Test Org")
                                .description("Description")
                                .build();

                OrganizationDto createdDto = OrganizationDto.builder()
                                .id(UUID.randomUUID())
                                .name("Test Org")
                                .description("Description")
                                .build();

                when(organizationService.createOrganization(any(OrganizationDto.class)))
                                .thenReturn(Mono.just(createdDto));

                webTestClient.post()
                                .uri("/api/accounting/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(dto)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody()
                                .jsonPath("$.data.name").isEqualTo("Test Org")
                                .jsonPath("$.data.id").isNotEmpty();
        }

        @Test
        @WithMockUser
        public void testGetOrganization() {
                UUID id = UUID.randomUUID();
                OrganizationDto dto = OrganizationDto.builder()
                                .id(id)
                                .name("Test Org")
                                .build();

                when(organizationService.getOrganization(id)).thenReturn(Mono.just(dto));

                webTestClient.get()
                                .uri("/api/accounting/organizations/{id}", id)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.data.id").isEqualTo(id.toString())
                                .jsonPath("$.data.name").isEqualTo("Test Org");
        }

        @Test
        @WithMockUser
        public void testGetAllOrganizations() {
                OrganizationDto dto1 = OrganizationDto.builder().name("Org 1").build();
                OrganizationDto dto2 = OrganizationDto.builder().name("Org 2").build();

                when(organizationService.getAllOrganizations()).thenReturn(Flux.just(dto1, dto2));

                webTestClient.get()
                                .uri("/api/accounting/organizations")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.data.length()").isEqualTo(2)
                                .jsonPath("$.data[0].name").isEqualTo("Org 1");
        }

        @Test
        @WithMockUser
        public void testUpdateOrganization() {
                UUID id = UUID.randomUUID();
                OrganizationDto dto = OrganizationDto.builder()
                                .name("Updated Org")
                                .build();

                when(organizationService.updateOrganization(eq(id), any(OrganizationDto.class)))
                                .thenReturn(Mono.just(dto));

                webTestClient.put()
                                .uri("/api/accounting/organizations/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(dto)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.data.name").isEqualTo("Updated Org");
        }

        @Test
        @WithMockUser
        public void testDeleteOrganization() {
                UUID id = UUID.randomUUID();

                when(organizationService.deleteOrganization(id)).thenReturn(Mono.empty());

                webTestClient.delete()
                                .uri("/api/accounting/organizations/{id}", id)
                                .exchange()
                                .expectStatus().isOk();
        }
}
