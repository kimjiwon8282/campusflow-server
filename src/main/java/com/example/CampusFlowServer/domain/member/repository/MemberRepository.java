package com.example.CampusFlowServer.domain.member.repository;

import com.example.CampusFlowServer.domain.member.entity.Member;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByLoginIdAndActiveTrue(String loginId);

    List<Member> findByLoginIdIn(Collection<String> loginIds);

    boolean existsByLoginId(String loginId);
}
