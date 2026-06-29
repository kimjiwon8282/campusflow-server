# CampusFlow - 대학교 수강신청 및 학사 수강 관리 시스템 API 서버

> 대학교 수강신청 흐름을 기반으로 인증·인가, 수강신청 정합성, 동시성 제어, 트랜잭션 경계 개선, Spring Batch 자동신청 처리를 구현한 백엔드 토이 프로젝트

<br>

## 1. 프로젝트 소개

CampusFlow는 대학교 수강신청과 학사 수강 관리를 위한 백엔드 프로젝트임.

학생은 개설 강의 조회, 희망과목 등록, 수강신청, 수강취소, 본인 수강 목록 조회를 수행할 수 있음.
교직원은 희망과목 자동신청 대상 사전 반영 Batch를 실행할 수 있음.

백엔드 서버는 학사 시스템 특성에 맞춰 관리자 등록 계정 기반 로그인, 역할별 API 접근 제어, 쿠키 기반 JWT 인증, 수강신청 정원 동시성 제어, 대기자 자동 승격, Spring Batch 기반 자동신청 사전 반영 기능을 담당함.

<br>

## 2. 담당 역할

**Spring Boot 기반 백엔드 설계 및 구현**

* 학생·교수·교직원 역할 기반 인증·인가 구조 구현
* AccessToken / RefreshToken 쿠키 기반 JWT 인증 구조 구현
* RefreshToken 해시 저장, Rotation, revoke 처리 구조 구현
* CSRF 토큰 발급 및 변경 요청 검증 흐름 구성
* 개설 강의 조회, 희망과목, 수강신청, 수강취소 API 구현
* 정원 초과 수강신청 문제 재현 및 비관적 락 기반 개선
* 트랜잭션 경계 분리를 통한 `CourseOffering` 락 보유 시간 단축
* Spring Batch 기반 희망과목 자동신청 사전 반영 구조 구현
* Scheduler 기반 수강신청 시작 D-1 자동 Batch 실행 구조 구현
* JUnit, MockMvc, k6 기반 정합성 및 성능 검증

<br>

## 3. 핵심 성과 요약

| 구분            | 개선 내용                                                           |
| ------------- | --------------------------------------------------------------- |
| 인증·인가 구조      | Spring Security 기반 쿠키 JWT 인증, RefreshToken 해시 저장, CSRF 보호 적용    |
| 수강신청 정합성 개선   | 정원 1명 강의에 20명 동시 신청 시 `ENROLLED 10명` 발생 문제 재현 및 개선              |
| 비관적 락 적용      | `CourseOffering` row 기준 `PESSIMISTIC_WRITE` 적용으로 정원 초과 수강 확정 방지 |
| 트랜잭션 경계 개선    | 사전 검증과 락 기반 Command 트랜잭션 분리로 p95 `3.52s → 1.67s` 개선             |
| 조회 패턴 보조 개선   | 대기 순번, 시간표 조회 패턴에 맞춘 복합 인덱스 추가                                  |
| 자동신청 Batch 처리 | Spring Batch와 Scheduler 기반 수강신청 D-1 희망과목 자동반영 구조 구현             |
| 중복 실행 방어      | JobParameter와 JobRepository 기반 동일 학기·일정 Batch 중복 실행 방어          |

<br>

## 4. 주요 기능

* 로그인, 토큰 재발급, 로그아웃
* AccessToken / RefreshToken 쿠키 기반 인증
* CSRF 토큰 발급 및 변경 요청 검증
* 학생·교수·교직원 역할 기반 API 접근 제어
* 학기별 개설 강의 조회 및 검색
* 희망과목 등록 및 자동신청 여부 설정
* 수강신청 및 대기 신청
* 수강취소 및 대기 1순위 자동 승격
* 본인 수강 목록 및 신청 학점 조회
* 교직원 자동신청 수동 실행 API
* Spring Batch 기반 희망과목 자동신청 사전 반영
* Scheduler 기반 수강신청 시작 D-1 자동 실행

<br>

## 5. 기술 스택

### Language & Framework

<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=Spring%20Security&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white">

### Database & Persistence

<img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/H2-09476B?style=for-the-badge&logo=h2database&logoColor=white"> <img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white">

### Auth & Test

<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"> <img src="https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white">

<br>

## 6. 핵심 기술 결정 및 트러블슈팅

수강신청 도메인에서 발생할 수 있는 인증 토큰 관리, 정원 초과 동시성 문제, 비관적 락 대기 지연, 희망과목 자동신청 대량 처리 문제를 직접 재현하고 구조를 개선함.

<br>

### 6.1. Spring Security 기반 인증·인가 구조

| 개선 항목           | 문제                            | 해결                                          | 결과                  |
| --------------- | ----------------------------- | ------------------------------------------- | ------------------- |
| 쿠키 기반 JWT 인증    | 로그인 이후 보호 API 인증 복원 필요        | AccessToken / RefreshToken을 HttpOnly 쿠키로 발급 | 쿠키 기반 인증 흐름 구성      |
| RefreshToken 저장 | RefreshToken 원문 저장 시 유출 위험 존재 | HMAC-SHA256 기반 tokenHash 저장                 | RefreshToken 원문 미저장 |
| 토큰 재발급          | 동일 RefreshToken 반복 사용 가능성     | RefreshToken Rotation 및 기존 토큰 revoke 처리     | 재발급 시 기존 토큰 재사용 차단  |
| CSRF 보호         | 쿠키 기반 인증에서 요청 위조 가능성 존재       | CSRF 토큰 쿠키와 요청 헤더 검증                        | 변경 요청 CSRF 검증       |
| 역할 기반 접근 제어     | 사용자 역할별 API 접근 제한 필요          | `@PreAuthorize` 기반 권한 검증                    | 역할별 API 접근 제어       |

#### 인증 흐름

```text
GET /api/v1/csrf
→ CSRF 토큰 생성 요청

POST /api/v1/auth/login
→ LoginFilter 인증
→ AccessToken / RefreshToken 발급
→ HttpOnly 쿠키 저장

보호 API 요청
→ JwtAuthenticationFilter 실행
→ AccessToken 검증
→ SecurityContext 인증 객체 저장

POST /api/v1/auth/reissue
→ RefreshToken 검증
→ 기존 RefreshToken revoke
→ 신규 AccessToken / RefreshToken 발급

POST /api/v1/auth/logout
→ RefreshToken revoke
→ 인증 쿠키 만료
```

RefreshToken은 DB에 원문을 저장하지 않고 HMAC-SHA256으로 해시 처리한 `tokenHash`만 저장함.
재발급 시 기존 RefreshToken을 폐기하고 신규 RefreshToken을 발급하는 Rotation 구조를 적용함.

AccessToken과 RefreshToken을 쿠키로 전달하기 때문에 CSRF 보호를 유지함.
로그인 전 `GET /api/v1/csrf` 요청으로 CSRF 토큰 생성을 유도하고, 변경 요청에서는 CSRF 토큰 쿠키와 요청 헤더를 검증함.

<br>

### 6.2. 수강신청 동시 요청 정합성 개선

초기 수강신청 로직은 현재 수강 확정 인원을 조회한 뒤 정원과 비교하고, `ENROLLED` 또는 `WAITING` 상태를 저장하는 구조였음.

```text
ENROLLED count 조회
→ capacity 비교
→ ENROLLED 또는 WAITING 저장
```

단일 요청에서는 정상 동작했지만, 여러 학생이 같은 강의에 동시에 신청할 경우 여러 트랜잭션이 같은 수강 확정 인원을 기준으로 판단할 수 있었음.

정원 1명 강의에 20명이 동시에 신청하는 테스트를 구성한 결과, 정원보다 많은 수강 확정 데이터가 생성됨.

| 구분     | ENROLLED | WAITING | 예외 |
| ------ | -------: | ------: | -: |
| 락 적용 전 |       10 |      10 |  0 |

이를 해결하기 위해 정원 판단 기준이 되는 `CourseOffering` row에 `PESSIMISTIC_WRITE` 락을 적용함.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select co from CourseOffering co where co.id = :id")
Optional<CourseOffering> findByIdForUpdate(Long id);
```

동일한 조건으로 10라운드 반복 테스트를 수행한 결과, 정원 초과 없이 수강 확정자와 대기자가 분리됨.

| 구분     | ENROLLED | WAITING | 예외 |
| ------ | -------: | ------: | -: |
| 락 적용 후 |        1 |      19 |  0 |

수강취소도 정원 정합성에 영향을 주기 때문에 동일한 `CourseOffering` row 기준으로 락을 적용함.
수강 확정자가 취소되면 대기 1순위를 조회하고, 학점과 시간표 조건을 다시 검증한 뒤 자동 승격 처리함.

<br>

### 6.3. 트랜잭션 경계 분리로 락 보유 시간 단축

비관적 락 적용으로 정원 정합성은 보장했지만, 동일 강의에 요청이 몰릴 경우 후순위 요청의 대기 시간이 증가할 수 있었음.

기존 구조에서는 학생 조회, 기간 검증, 중복 검증, 학점 검증, 시간표 검증, 정원 판단, 저장, 대기번호 조회가 하나의 긴 트랜잭션 안에서 실행됨.
이 경우 `CourseOffering` 락을 잡은 뒤 수행되는 작업이 많아져 락 보유 시간이 길어질 수 있음.

개선 후에는 사전 검증과 락 기반 저장 구간을 분리함.

```text
외부 apply 흐름
→ NOT_SUPPORTED

사전 검증
→ readOnly 트랜잭션

락 기반 Command
→ REQUIRES_NEW
→ CourseOffering 락 획득
→ 최종 검증
→ 정원 판단
→ Enrollment 저장
→ flush
→ commit

응답 생성
→ waitNo 조회
```

`CourseOffering` 락 조회를 Command 트랜잭션의 첫 DB 조회로 두고, 사전 검증 결과는 엔티티가 아닌 ID 기반으로 전달함.
대기번호 조회와 응답 생성은 Command 커밋 이후 수행하도록 분리함.

k6를 사용해 동일 강의에 100명이 동시에 신청하는 상황을 검증함.

| 지표       |  개선 전 |  개선 후 |         개선 |
| -------- | ----: | ----: | ---------: |
| 평균 응답 시간 | 1.95s | 1.45s | 약 25.6% 감소 |
| p95      | 3.52s | 1.67s | 약 52.6% 감소 |
| p99      | 3.65s | 1.73s | 약 52.6% 감소 |
| max      | 3.66s | 1.75s | 약 52.2% 감소 |

정합성 검증 결과도 함께 유지됨.

```text
capacity = 10
동시 요청 = 100
ENROLLED = 10
WAITING = 90
HTTP 실패율 = 0%
```

<br>

### 6.4. 조회 패턴 기반 복합 인덱스 추가

대기 순번 계산과 시간표 충돌 검증에서 반복 조회되는 컬럼 조합을 기준으로 복합 인덱스를 엔티티 스키마에 추가함.

```text
Enrollment
→ course_offering_id, status, applied_at, id

CourseTime
→ course_offering_id, day_of_week, start_period
```

k6 기준 동일 조건 비교 결과, 응답 시간이 소폭 개선됨.

| 지표       | 인덱스 적용 전 | 인덱스 적용 후 |
| -------- | -------: | -------: |
| 평균 응답 시간 |    1.32s |    1.23s |
| p95      |    2.45s |    2.37s |
| p99      |    2.55s |    2.47s |
| max      |    2.58s |    2.49s |

복합 인덱스는 핵심 병목 해결보다, 대기 순번 조회와 시간표 조회 패턴을 보조하는 개선으로 적용함.

<br>

### 6.5. Spring Batch 기반 희망과목 자동신청 사전 반영

수강신청 시작 전날, 자동신청 대상으로 확정된 희망과목을 미리 수강신청으로 반영하는 Batch 구조를 구현함.

```text
Scheduler
→ ENROLLMENT.startAt 기준 D-1 학기 탐색
→ AutoEnrollmentBatchLaunchService 호출

Batch
→ autoApply=true, result=DONE WishCourse 조회
→ Writer에서 AutoEnrollmentCommandService.applyOne() 호출

Command
→ CourseOffering 비관적 락 획득
→ 자동반영 대상 재검증
→ 학점·시간표·정원·중복 검증
→ Enrollment 생성 또는 재활성화
```

Batch 구성.

```text
Job
→ autoEnrollmentPreApplyJob

Step
→ applyDoneWishCoursesStep

Reader
→ JpaPagingItemReader
→ wishCourseId, courseOfferingId projection 조회

Writer
→ AutoEnrollmentCommandService.applyOne() 호출

Chunk size
→ 20
```

Reader는 전체 엔티티가 아닌 자동반영에 필요한 `wishCourseId`, `courseOfferingId`만 조회함.
Writer는 직접 Enrollment 저장 로직을 가지지 않고, 단건 Command를 호출함.

자동반영 시점에는 이미 활성 수강신청이 있는지, 학점이 초과되는지, 시간표가 충돌하는지, 정원이 남아 있는지 다시 검증함.
정원 초과, 시간표 충돌, 학점 초과 등 부적격 대상은 Enrollment를 생성하지 않고 skip 상태로 집계함.

Batch 실행 시 `semesterId`, `academicYear`, `term`, `enrollmentStartAt`, `businessDate`를 JobParameter로 구성함.
동일 학기와 일정 기준으로 이미 완료된 Job은 다시 실행하지 않고 skip 처리함.

<br>

## 7. 테스트 및 검증

* MockMvc 기반 인증 통합 테스트
* CSRF, 로그인, 재발급, 로그아웃 검증
* 역할별 API 접근 제한 검증
* 수강신청 기본 흐름 검증
* 중복 신청, 시간표 충돌, 최대 학점 초과, 기간 외 신청 차단 검증
* CountDownLatch 기반 동시성 테스트
* k6 기반 HTTP 동시 요청 테스트
* Spring Batch 실행 및 중복 실행 방지 검증
* Scheduler D-1 대상 탐색 검증

<br>

## 8. 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 인증 테스트
./gradlew test --tests "*AuthIntegrationTest"

# 수강신청 동시성 테스트
./gradlew test --tests "*StudentEnrollmentConcurrencyIntegrationTest"

# 수강신청 통합 테스트
./gradlew test --tests "*StudentEnrollmentIntegrationTest"
```

<br>

## 9. 팀원 및 역할

| 이름      | 역할      | 담당 내용                                                                                    |
| ------- | ------- | ---------------------------------------------------------------------------------------- |
| **김지원** | Backend | Spring Boot 기반 API 설계 및 구현, 인증·인가, 수강신청 정합성 개선, 비관적 락 및 트랜잭션 경계 개선, Spring Batch 자동신청 처리 |
