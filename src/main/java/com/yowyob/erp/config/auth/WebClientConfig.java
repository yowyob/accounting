package com.yowyob.erp.config.auth;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration des clients HTTP du BACKEND.
 *
 * Deux beans :
 *  1. kernelWebClient (primary) — Appels vers KSM_Kernel_Layer.
 *     Injecte automatiquement X-Client-Id et X-Api-Key pour que le Kernel
 *     identifie le BACKEND comme une ClientApplication enregistrée.
 *
 *  2. webClient — Client générique sans headers Kernel (usage interne).
 */
@Configuration
public class WebClientConfig {

    @Value("${auth.api.timeout:5000}")
    private int timeout;

    /** Identifiant de la ClientApplication du BACKEND dans le Kernel */
    @Value("${kernel.client.id}")
    private String kernelClientId;

    /** Secret de la ClientApplication du BACKEND dans le Kernel */
    @Value("${kernel.client.secret}")
    private String kernelClientSecret;

    @Value("${auth.api.url}")
    private String kernelBaseUrl;

    /**
     * WebClient principal pour toutes les communications avec le Kernel.
     *
     * Headers injectés automatiquement sur chaque requête :
     *   X-Client-Id : identifie le BACKEND comme ClientApplication
     *   X-Api-Key   : authentifie le BACKEND auprès du Kernel
     */
    @Bean
    @Primary
    public WebClient kernelWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout));

        return WebClient.builder()
                .baseUrl(kernelBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("X-Client-Id", kernelClientId)
                .defaultHeader("X-Api-Key",   kernelClientSecret)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * WebClient générique sans headers Kernel (pour d'autres appels HTTP externes).
     */
    @Bean("genericWebClient")
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
