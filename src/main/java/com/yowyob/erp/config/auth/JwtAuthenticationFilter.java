package com.yowyob.erp.config.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final AuthService authService;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token != null) {
            return authService.validateToken(token)
                    .flatMap(authResponse -> {
                        if (authResponse.isValid()) {
                            UsernamePasswordAuthenticationToken authentication = createAuthentication(authResponse);
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                        } else {
                            return chain.filter(exchange);
                        }
                    })
                    .onErrorResume(error -> {
                         log.error("Error validating token", error);
                         return chain.filter(exchange);
                    });
        }

        return chain.filter(exchange);
    }

    private String extractToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null; // Return null if not found or invalid format
    }

    private UsernamePasswordAuthenticationToken createAuthentication(AuthValidationResponse authResponse) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(authResponse.getRoles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(authResponse.getUserId(), null, authorities);
    }
}