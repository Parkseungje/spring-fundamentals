# CLAUDE.md — spring-fundamentals

이 프로젝트에서 작업할 때 따를 컨벤션.

## 목적
"자바 → 스프링 통합 마스터 커리큘럼"의 PART 8~21을 Spring Boot 코드로 직접 검증한다.
단순히 동작하는 코드를 만드는 게 아니라, **Spring의 내부 동작(빈 등록, 프록시, SQL 로그, 빈 후처리기 등)을
로그·디버거·H2 콘솔·actuator로 직접 들여다보는 것**이 핵심.

## 패키지 컨벤션
- `com.study.partNN_topic` — PART 번호(2자리) + 주제. 커리큘럼의 PART 번호와 정확히 일치.
- "고통 → 해결" 서사가 있는 단원은 `traditional/`(Before) / `improved/`(After, Spring 적용)로 분리해
  같은 기능을 두 가지 방식으로 구현하고 차이를 코드와 로그로 비교한다.
  예: PART 11.5 트랜잭션 — `traditional`(수동 commit/rollback) vs `improved`(`@Transactional`)

## 코드 작성 원칙
- 빈/엔티티/컨트롤러는 실제로 `bootRun` 또는 슬라이스 테스트로 동작을 확인할 수 있게 작성
- PART 14(JPA)와 PART 15~16(인덱스)은 **반드시 `show-sql`/`format_sql` 로그를 켜고** 실제 쿼리를 확인한 뒤 docs에 기록
- PART 12(AOP)는 등록된 빈이 프록시(`$Proxy..` 또는 CGLIB)인지 `getClass()`로 직접 확인하는 코드를 남긴다
- 트랜잭션·AOP 관련 "함정" 단원(13.3 internal call, 13.4 @PostConstruct)은
  반드시 함정이 실제로 재현되는 실패 케이스를 코드로 만들고, 그다음 해결 버전을 작성

## docs/ 작성 규칙
- 파일명: `PARTNN_주제.md`
- 포함할 것: 직접 실행해서 얻은 로그/SQL/스택트레이스 발췌, 자기 점검 질문에 대한 답, 예상과 다른 동작 기록

## 빌드/실행
- Spring Boot 3.3.x, Java 21, Gradle (Groovy DSL)
- H2 인메모리 DB 기본 (`application.yml`), 운영 DB 특화 실습이 필요하면 Testcontainers로 전환 (PART 20.6)
- `./gradlew bootRun --args='--debug'`로 자동 설정 리포트 확인 가능
