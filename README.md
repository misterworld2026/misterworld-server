# 미스터월드 Spring Boot 백엔드 — 구현 진행본

현재는 **인증·회원·상품/옵션·일정 관리·재고·조회 기능이 실행 가능한 단계**다. 전체 여행 신청 서비스 완료본이 아니다. 허니문 출발·인원 합산·옵션 등급 변경·단골 할인·문자 시점·재고 기준은 사용자 결정을 기록했다. 가격 계산·신청 단위·마감·취소·직원 발급 등 남은 정책에 대한 답변이 필요하다. 현재 결정을 임의 적용하지 않기 위해 `/api/reservations`와 고객 신청 취소는 HTTP 409 `POLICY_UNRESOLVED`를 반환한다. 직원 생성도 같은 방식으로 차단한다.

- 원문 검토·충돌·질문: [docs/REVIEW.md](docs/REVIEW.md)
- 원문 명칭·소속·필드·코드 위치: [docs/NAME_REGISTER.md](docs/NAME_REGISTER.md)
- 분석 그림 메시지(메서드와 구별): [docs/MESSAGE_REGISTER.md](docs/MESSAGE_REGISTER.md)
- API 계약·화면→DB→테스트 대응: [docs/API.md](docs/API.md)
- 실제 검증·미검증: [docs/VALIDATION.md](docs/VALIDATION.md)
- 빈 DB 스키마: `src/main/resources/db/migration/V1__empty_database.sql`
- 인증 세션·조회 인덱스: V2, V3 마이그레이션

## 환경과 실행

기존 Java 21, Spring Boot 4.1.1, Gradle 9.7.1 유지. MySQL 드라이버는 Boot 의존성 관리에 따른다. springdoc 2.8.0은 Boot 4와 호환되지 않아 3.1.1로 수정했다. 공식 근거: https://springdoc.org/index.html .

```powershell
.\gradlew.bat test bootJar
java -jar build/libs/misterworld-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

실제 JAR 이름은 `build/libs`에서 확인한다. local 프로필은 별도의 메모리 H2 DB이며 종료하면 데이터가 사라진다. local JWT 키는 공개 개발용 문자열이므로 운영에 사용할 수 없다. 테스트용 STAFF 생성은 테스트 코드에서만 수행하며, 실제 계정 발급은 정책 결정 전 비활성이다. 일반 가입·로그인은 추가 API `/api/auth/signup`, `/api/auth/login`을 이용한다. 상품 조회는 분석 PDF의 로그인 사전 조건에 따라 인증을 요구한다. 비로그인 조회는 아직 합의하지 않았다.

MySQL은 새 빈 데이터베이스를 준비하고 다음 환경변수를 설정한다.

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/misterworld?characterEncoding=UTF-8'
$env:DB_USERNAME='misterworld'
$env:DB_PASSWORD='<운영 환경에서 주입>'
$env:JWT_SECRET='<32 UTF-8 바이트 이상의 충분히 무작위인 비밀값>'
java -jar build/libs/misterworld-server-0.0.1-SNAPSHOT.jar
```

`.env`은 문서용이다. 애플리케이션이 `.env`를 자동으로 읽지는 않는다. 환경변수로 주입한다. Swagger는 `/swagger-ui.html`, 계약 JSON은 `/v3/api-docs`다.

## 데이터 보존과 보안

- Flyway `baseline-on-migrate=false`, Hibernate `ddl-auto=validate`; 기존 DB에 자동 drop·재생성을 하지 않는다.
- **V1은 실제 기존 DDL을 바탕으로 검증한 ALTER 스크립트가 아니다.** 프롬프트의 한국어 컬럼 목록을 출발점으로 만든 빈 DB 전용 스키마다. 기존 비어 있지 않은 DB는 Flyway가 자동 baseline하지 않고 실패한다. 실제 DDL·기존 데이터 수령 후 별도 무손실 마이그레이션이 필요하다.
- 회원 `Field`는 PDF `travelCount`에 매핑한다. 미완성 `Untitled`의 데이터를 자동 변환하지 않는다. 신규 `여행객` 테이블은 PDF TravelParticipant를 위한 추가 테이블이다.
- JWT HS256, 액세스 15분·리프레시 14일, 세션 저장 및 갱신 시 회전·이전 세션 회수는 보안을 위한 추가 설계다. 로그아웃 시 동일 세션 액세스·리프레시 모두 회수된다. 다른 세션은 유지한다.
- 공개 요청에서 역할을 지정할 수 없다. 본인 ID는 인증 정보에서 가져온다. 비밀번호는 BCrypt, 저장 리프레시는 SHA-256 해시다. 요청 DTO의 toString이나 토큰을 로그에 남기는 코드는 없다.
- 신청·알림·여행객 관계에 cascade 삭제를 설정하지 않았다. 이메일 UNIQUE, FK, 상태 CHECK, 수량·가격 CHECK를 작성했다. MySQL 실제 실행은 미검증이다.

## 남은 업무

1. 사용자 정책 답변과 기준 PDF 확정, HTML·DDL·API 이미지 위치 확인.
2. 명칭 대장의 모든 서명·DTO·메시지 분류를 원문과 대조 완료하고 정확한 호환 계층 구성.
3. 견적·신청(여행객·다중 옵션·상품 구성/금액 스냅샷·멱등키) 트랜잭션.
4. 출발 확정·신청/일정 취소·영속 알림/outbox와 멀티 인스턴스 발송 worker.
5. 자동 상태 전환·장기 중단 복구·완료 횟수 원장과 할인.
6. 직원 예약 현황/대시보드, 외부 음성·SMS 실연동, 합의 시 결제.
7. 실제 MySQL 동시성·제약·마이그레이션·복구 시나리오 검증.

외부 SMS·음성은 현재 명시적인 미연결 어댑터다. SMS 발송·재시도/outbox 구현이 있다고 주장하지 않는다. 음성 `/api/voice/interpret`는 503을 반환하며, 미언급/실패 분리와 선택 검증 로직만 단위 검증한다. 결제·환불 API는 구현하지 않았다.

별도로 `/api/v1/voice/transcriptions`와 `/api/v1/voice/interpretations`는 Python 음성 서비스와 연결된 회원용 API다. 실행 방법과 요청·응답 계약은 [voice-service/README.md](voice-service/README.md)와 [voice-service/CONTRACT.md](voice-service/CONTRACT.md)를 참고한다.


## Lombok 적용

Gradle compileOnly/annotationProcessor에 Lombok을 추가했다. 버전은 기존 Spring Boot 의존성 관리에 맡긴다. 공식 설정 참고: https://projectlombok.org/setup/gradle .

Entity 및 단순 상태 객체는 `@Getter`, 기존 setter가 있던 필드만 `@Setter`를 사용한다. ID setter와 새로운 상태 변경 API를 추가하지 않는다. Controller·Service의 단순 주입 생성자는 `@RequiredArgsConstructor`로 생성한다. JwtProvider의 키 검증 생성자, TokenSession의 생성/회수 로직, 업무 메서드는 유지한다. record DTO에는 Lombok을 추가하지 않는다. `@Data`/자동 toString은 사용하지 않는다.


2026-11-03 사용자 확정 사항은 `docs/REVIEW.md`의 사용자 결정 표를 따른다. 재고 부족 기준은 3개 이하이며 V4로 저장 상태도 재분류한다. PDF의 기존 4개 이하 표기를 원문에서 바꾸지는 않는다.
