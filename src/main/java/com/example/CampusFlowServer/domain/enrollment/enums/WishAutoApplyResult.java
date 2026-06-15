package com.example.CampusFlowServer.domain.enrollment.enums;

public enum WishAutoApplyResult {
    NOT_SELECTED, //자동 신청 미선택
    PENDING, //자동신청 선택 후 결과 확인 전
    DONE, // 현재 기준 자동 신청 가능 상태
    OVER_CAPACITY, //정원 초과로 본 신청 필요
    NEEDS_MANUAL, //본 수강신청 필요
    TIME_CONFLICT //시간표 충돌로 본 신청 필요
}
