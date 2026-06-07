# CLAUDE.md — spring-fundamentals

이 프로젝트에서 작업할 때 따를 컨벤션.

## 참고 문서
전체 커리큘럼 원본은 `../master_curriculum.md` (`C:\develop\study\master_curriculum.md`)에 있다.
이 프로젝트는 그중 PART 8~21을 다룬다. PART 번호·소단원 번호·"자기 점검"/"졸업 관문" 질문은
모두 이 파일을 기준으로 하며, docs/ 노트 작성 시에도 이 문서의 구조(고통→해결, 기술 진화의 척추 표)를 따른다.
이 파일은 두 프로젝트가 공유하므로 프로젝트 내부로 복사하지 않는다.

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

## 버전 주의 (Spring Boot / Java)
- 이 프로젝트는 **Spring Boot 3.3.x / Java 21**로 고정되어 있다 (`build.gradle`).
- 커리큘럼 예제 중에는 **Spring(Boot) 메이저 버전에 따라 동작·기본값·설정 방식이 달라지는 부분**이 많다.
  대표적으로:
  - PART 12 AOP: Spring Boot 3.x는 기본적으로 CGLIB 프록시 사용 (`proxyTargetClass` 기본값 차이)
  - PART 19 Security: Spring Security 5.x(`WebSecurityConfigurerAdapter`) → 6.x(`SecurityFilterChain` 빈 등록) 설정 방식 자체가 변경됨
  - PART 3 GC: JDK 버전마다 기본 GC가 다름 (8=Parallel, 9+=G1)
  - PART 14 JPA/Hibernate: 메이저 버전에 따라 기본 fetch 전략·SQL 생성 방식에 차이가 있을 수 있음
- 새 예제를 작성하거나 커리큘럼 설명과 실제 동작이 다르면, **"Spring Boot 3.3.x / Java 21 기준" 임을 코드 주석 또는 docs에 명시**하고
  구버전 자료(예: Spring 5.x 기반 강의/문서)와 비교가 필요하면 그 차이를 docs에 별도로 기록한다.

## docs/ 작성 규칙
- 파일명: `PARTNN_주제.md`
- 포함할 것: 직접 실행해서 얻은 로그/SQL/스택트레이스 발췌, 자기 점검 질문에 대한 답, 예상과 다른 동작 기록

## 빌드/실행
- Spring Boot 3.3.x, Java 21, Gradle (Groovy DSL)
- H2 인메모리 DB 기본 (`application.yml`), 운영 DB 특화 실습이 필요하면 Testcontainers로 전환 (PART 20.6)
- `./gradlew bootRun --args='--debug'`로 자동 설정 리포트 확인 가능
