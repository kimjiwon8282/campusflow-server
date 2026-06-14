package com.example.CampusFlowServer.domain.auth.security;

import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomMemberDetails implements UserDetails {

    private final Long memberId;
    private final String loginId;
    private final String password;
    private final String name;
    private final MemberRole role;
    private final boolean active;

    public CustomMemberDetails(Member member) {
        this(
            member.getId(),
            member.getLoginId(),
            member.getPassword(),
            member.getName(),
            member.getRole(),
            member.isActive()
        );
    }

    private CustomMemberDetails(
        Long memberId,
        String loginId,
        String password,
        String name,
        MemberRole role,
        boolean active
    ) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.active = active;
    }

    public static CustomMemberDetails fromToken(
        Long memberId,
        String loginId,
        String name,
        MemberRole role
    ) {
        return new CustomMemberDetails(memberId, loginId, "", name, role, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
