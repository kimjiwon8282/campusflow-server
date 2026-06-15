package com.example.CampusFlowServer.domain.member.repository;

import com.example.CampusFlowServer.domain.member.entity.StaffProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<StaffProfile> findByMember_LoginId(String loginId);
}
