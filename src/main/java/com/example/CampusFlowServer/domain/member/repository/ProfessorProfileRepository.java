package com.example.CampusFlowServer.domain.member.repository;

import com.example.CampusFlowServer.domain.member.entity.ProfessorProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorProfileRepository extends JpaRepository<ProfessorProfile, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<ProfessorProfile> findByMember_LoginId(String loginId);
}
