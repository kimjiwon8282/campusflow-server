package com.example.CampusFlowServer.domain.member.repository;

import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<StudentProfile> findByMember_LoginId(String loginId);

    List<StudentProfile> findByMember_LoginIdIn(Collection<String> loginIds);
}
