# 실제 검증 기록

2026-11-03, Java 21 환경. 실행하지 않은 시나리오는 통과로 간주하지 않는다.

## 실행 결과

1. 변경 전 `gradlew.bat test --no-daemon`: 실패. 기존 contextLoads가 DB URL/드라이버 구성 부재로 DataSource 생성 실패.
2. 구현 후 HTTP 검증: 재고 1.5가 1로 변환되어 테스트 실패. Jackson accept-float-as-int=false로 수정.
3. 이전 구현 시점 `gradlew.bat test bootJar --no-daemon`: **BUILD SUCCESSFUL**, 테스트 10개, 실패 0, 오류 0, 건너뜀 0. Spring Boot HTTP 서버·H2·Flyway V1/V2/V3·Hibernate validate를 실제 기동.

| 테스트 클래스 | 테스트 수 | 검증 내용 |
|---|---:|---|
| MisterworldServerApplicationTests | 1 | local 프로필 컨텍스트 기동 |
| ApiIntegrationTests | 2 | 가입/중복/역할 주입 차단, 해시 저장, 로그인, 직원 권한, 401/403/404/405, 본인 프로필, refresh 회전·재사용 거부·이전 access 거부, logout 회수, 정책 미결정 신청 409, 상품 등록, 허니문 클래식 거부, 재고 -1/0/4/5/1.5, 페이지 크기, 일정 등록·삭제 |
| NameContractTests | 2 | 클래스 소속·필드 타입·한국어 컬럼 매핑, 일부 메서드의 실제 리플렉션 선언 대조 |
| ProductSelectionTests | 3 | 허니문·효도 클래식 제한, 일반 테마 허용, 중복·타 상품·미해결 옵션 거부, 클래식 투어의 프리미엄 식사 선택 |
| VoiceServiceTests | 2 | 미언급은 추론 실패 아님, 실패 항목 수 집계, 미해결 필드·확인 요구 |

테스트 케이스 개수는 시나리오 개수와 같지 않다. 한 테스트가 여러 HTTP 요청을 검증한다. NameContractTests는 전체 명칭 대장 검증이 아니라 선택된 선언의 검증이다. 아직 전체 일치라고 보고할 수 없다.

실행 리포트: `build/reports/tests/test/index.html`, XML: `build/test-results/test/TEST-*.xml`. 산출 JAR: `build/libs/*-0.0.1-SNAPSHOT.jar` (plain 제외).

## 실행 JAR 검증

`python tools/smoke_jar.py` 실행 성공. 최신 JAR를 local 프로필로 별도 프로세스 기동, `/v3/api-docs` HTTP 200 및 경로 27개 확인, `/api/my/profile` 비인증 HTTP 401 확인 후 프로세스 종료. OpenAPI 노출은 각 기능의 업무 동작 완료를 뜻하지 않는다.

## MySQL·동시성 검증 제한

Docker CLI는 있지만 엔진 파이프가 없어 실행 불가. MySQL 클라이언트/접속 환경변수도 미확인. 실제 MySQL 버전·기존 데이터는 확인하지 못했다. 따라서 MySQL 마이그레이션 성공·PESSIMISTIC_WRITE 정원 경합·데드락·유니크 제약 경합 통과를 주장하지 않는다. H2 MySQL 모드는 MySQL 대체 증거가 아니다.

## 미실행·미구현 시나리오

- 가격 단위·차액·반올림, 0/1/2회 할인 경계, D-7·자정·업무 시간대.
- 일반 2/3명, 허니문 팀/정원, 다른 구성 인원 합산, 정원 직전 동시 신청, 멱등 재전송.
- 고객 취소·확정 경합, 일정 취소·신규 신청 경합, 신청 기록 존재 일정 보존·상품 수정 후 스냅샷.
- 최초/추가 알림 분기, 실패·타임아웃·중복·하루 1회 재시도, 커밋 직후 중단·재시작 복구.
- 완료 처리 중복, 장기 중단 후 자동 상태 전환 복구, 회원별 동일 일정 1회 원장.
- 실제 타인 신청·알림 데이터가 있는 경우의 분리, 외부 SMS/음성 실연동, 결제/환불.

이 항목들은 테스트를 건너뛰어 통과시킨 것이 아니라 기능·정책 구현이 남아 있는 작업이다.


## 사용자 정책 확정 이후 검증 (2026-11-03)

- 재고 기준: 0 OUT_OF_STOCK / 1~3 LOW_STOCK / 4 이상 NORMAL. HTTP 테스트에서 -1·0·1·3·4·5·1.5 확인.
- V4 스크립트가 기존 수량 4·LOW_STOCK 데이터를 NORMAL로 재분류하고 수량·수정일시를 보존하는지 H2 통합 테스트로 확인.
- 완료 여행 0·1회 할인율 0%, 2·3회 10%, 음수 거부 단위 테스트 추가. 이는 할인율 판정이며 신청 금액 할인/완료 횟수 자동 반영 검증이 아니다.
- `gradlew.bat test bootJar --no-daemon --offline` 성공, 새 마이그레이션 테스트 추가 후 `gradlew.bat test --no-daemon --offline` 재실행 성공.
- 최종 테스트 **12개, 실패 0, 오류 0, 건너뜀 0**. 실제 MySQL 검증은 여전히 미실행.
