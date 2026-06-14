package com.example.CampusFlowServer.domain.auth.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    @GetMapping
    public Map<String, String> issueCsrfToken() {
        return Map.of("message", "CSRF token issued");
    }
}
