package com.example.CampusFlowServer.dev.auth.init;

import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import com.example.CampusFlowServer.domain.member.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
public class TestAccountSeeder implements ApplicationRunner {

    private static final String RAW_PASSWORD = "1234";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public TestAccountSeeder(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("stu001", "학생", MemberRole.STUDENT);
        createIfAbsent("prof001", "교수", MemberRole.PROFESSOR);
        createIfAbsent("staff001", "교직원", MemberRole.STAFF);
    }

    private void createIfAbsent(String loginId, String name, MemberRole role) {
        if (memberRepository.existsByLoginId(loginId)) {
            return;
        }

        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);
        Member member = Member.create(loginId, encodedPassword, name, role);
        memberRepository.save(member);
    }
}
