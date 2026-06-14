package com.example.CampusFlowServer.domain.member.controller;

import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.member.dto.MemberMeResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public MemberMeResponse me(@AuthenticationPrincipal CustomMemberDetails principal) {
        return new MemberMeResponse(
            principal.getMemberId(),
            principal.getLoginId(),
            principal.getName(),
            principal.getRole().name()
        );
    }
}
