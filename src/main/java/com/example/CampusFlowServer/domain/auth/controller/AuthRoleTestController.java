package com.example.CampusFlowServer.domain.auth.controller;

import com.example.CampusFlowServer.domain.auth.dto.AuthMessageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Temporary endpoints for verifying method-security role rules before domain APIs exist.
@RestController
@RequestMapping("/api/v1/auth/role-test")
public class AuthRoleTestController {

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student")
    public AuthMessageResponse student() {
        return new AuthMessageResponse("STUDENT access granted");
    }

    @PreAuthorize("hasRole('PROFESSOR')")
    @GetMapping("/professor")
    public AuthMessageResponse professor() {
        return new AuthMessageResponse("PROFESSOR access granted");
    }

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/staff")
    public AuthMessageResponse staff() {
        return new AuthMessageResponse("STAFF access granted");
    }
}
