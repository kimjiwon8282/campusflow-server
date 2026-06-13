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
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.name = member.getName();
        this.role = member.getRole();
        this.active = member.isActive();
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
