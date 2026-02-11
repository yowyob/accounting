// Contrôleur de santé et informations système
package com.yowyob.erp.common.controller;

import com.yowyob.erp.common.dto.ApiResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String port;

    @GetMapping("/health")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", applicationName);
        health.put("port", port);
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(ApiResponseWrapper.success(health, "Service opérationnel"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", applicationName);
        info.put("version", "1.0.0");
        info.put("description", "Yowyob ERP - Module Comptabilité");
        info.put("phase", "Phase 1 - Configuration et Architecture Multi-organization");
        
        return ResponseEntity.ok(ApiResponseWrapper.success(info));
    }
}
