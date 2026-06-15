package com.example.CampusFlowServer.domain.student.wishcourse.controller;

import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseAutoApplyCheckRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseAutoApplyRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCandidateResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCandidateSearchRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCreateRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseItemResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseListResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseSemesterRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.service.StudentWishCourseService;
import com.example.CampusFlowServer.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학생 희망과목", description = "학생 희망과목 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/wish-courses")
public class StudentWishCourseController {

    private final StudentWishCourseService studentWishCourseService;

    @GetMapping("/candidates")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "희망과목 담기 대상 강의 조회",
        description = "학년도와 학기를 기준으로 희망과목 담기 대상 강의를 조회한다. 조회 기능이며 실제 수강신청 확정이 아니다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public List<StudentWishCourseCandidateResponse> searchCandidates(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @ParameterObject @ModelAttribute StudentWishCourseCandidateSearchRequest request
    ) {
        return studentWishCourseService.searchCandidates(memberDetails.getMemberId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "내 희망과목 목록 조회",
        description = "현재 로그인한 학생의 희망과목 목록과 요약을 조회한다. 조회 기능이며 실제 수강신청 확정이 아니다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentWishCourseListResponse getMyWishCourses(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @ParameterObject @ModelAttribute StudentWishCourseSemesterRequest request
    ) {
        return studentWishCourseService.getMyWishCourses(memberDetails.getMemberId(), request);
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "희망과목 담기",
        description = "개설 강의를 희망과목으로 담는다. 실제 수강신청 확정이 아니며, CSRF 헤더가 필요하다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentWishCourseItemResponse createWishCourse(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @RequestBody StudentWishCourseCreateRequest request
    ) {
        return studentWishCourseService.createWishCourse(memberDetails.getMemberId(), request);
    }

    @DeleteMapping("/{wishCourseId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "희망과목 삭제",
        description = "본인의 희망과목을 삭제한다. 실제 수강취소가 아니며, CSRF 헤더가 필요하다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public ResponseEntity<Void> deleteWishCourse(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @PathVariable Long wishCourseId
    ) {
        studentWishCourseService.deleteWishCourse(memberDetails.getMemberId(), wishCourseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{wishCourseId}/auto-apply")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "희망과목 자동신청 선택/해제",
        description = "희망과목의 자동신청 선택 여부를 변경한다. 실제 수강신청 확정이 아니며, CSRF 헤더가 필요하다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentWishCourseItemResponse updateAutoApply(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @PathVariable Long wishCourseId,
        @RequestBody StudentWishCourseAutoApplyRequest request
    ) {
        return studentWishCourseService.updateAutoApply(
            memberDetails.getMemberId(),
            wishCourseId,
            request
        );
    }

    @PostMapping("/auto-apply/check")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "희망과목 자동신청 결과 확인",
        description = "현재 기준으로 희망과목 자동신청 가능 여부를 확인한다. 신청 가능 학년 제한, 선수과목 이수 여부, 시간표 충돌, 정원 초과 여부를 확인하지만 실제 수강신청 확정이 아니며 Enrollment를 생성하지 않고 정원 또는 신청 인원을 변경하지 않는다. 결과는 현재 시점 기준이므로 다른 학생의 희망과목 선택에 따라 달라질 수 있으며, CSRF 헤더가 필요하다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentWishCourseListResponse checkAutoApplyResults(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @RequestBody StudentWishCourseAutoApplyCheckRequest request
    ) {
        return studentWishCourseService.checkAutoApplyResults(
            memberDetails.getMemberId(),
            request
        );
    }
}
