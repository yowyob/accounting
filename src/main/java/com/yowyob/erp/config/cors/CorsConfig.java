package com.yowyob.erp.config.cors;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

   @Bean
   public CorsWebFilter corsFilter() {
       CorsConfiguration corsConfiguration = new CorsConfiguration();
       // Allow all origins for now to facilitate dev/docker access
       corsConfiguration.addAllowedOriginPattern("*"); 
       corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
       corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
       corsConfiguration.setAllowCredentials(true);

       UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
       source.registerCorsConfiguration("/**", corsConfiguration);
       return new CorsWebFilter(source);
   }
}
