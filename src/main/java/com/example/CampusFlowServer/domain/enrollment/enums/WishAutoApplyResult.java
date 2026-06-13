package com.example.CampusFlowServer.domain.enrollment.enums;

public enum WishAutoApplyResult {
    NOT_SELECTED, //자동 신청 미선택
    PENDING, //자동신청 선택 후 처리 대기
    DONE, //자동 신청 완료
    OVER_CAPACITY, //정원초과로 미처리
    NEEDS_MANUAL, //본 수강신청 필요
    TIME_CONFLICT //시간표 중복으로 미처리
}