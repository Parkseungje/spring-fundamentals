# spring-fundamentals

자바 → 스프링 통합 마스터 커리큘럼 중 **PART 8~21 (IoC/DI, AOP, DB 접근, JPA, MVC, 분산 시스템, Security, 테스트, DevOps)** 학습용 Spring Boot 프로젝트.

> 📌 전체 커리큘럼 원본: [`../master_curriculum.md`](../master_curriculum.md) (`C:\develop\study\master_curriculum.md`)
> — `java-fundamentals`와 공유하는 참고 문서이므로 이 프로젝트 내부로 복사하지 않는다.

## 환경
- **Spring Boot 3.3.x / Java 21** (`build.gradle` 참고) — 정상 실행 확인됨
  - 커리큘럼 예제 중 일부는 **Spring(Boot)·Java 버전에 따라 동작·설정 방식이 달라질 수 있다**
    (예: PART 12 AOP — Boot 3.x는 기본 CGLIB / PART 19 Security — 5.x→6.x `SecurityFilterChain` 설정 방식 변경 /
    PART 3 GC 기본값은 JDK 버전마다 다름 등). 버전 의존적인 내용은 docs에 "Spring Boot 3.3.x / Java 21 기준"으로 명시할 것

## 스택
- Spring Boot 3.3.x / Java 21 / Gradle
- H2 (인메모리 DB), Spring Data JPA, Spring Web, Spring AOP
- `application.yml`에 `show-sql: true` + Hibernate SQL 로그 활성화 (PART 14 N+1 관찰용)

## 구조

```
src/main/java/com/study/partNN_topic/
    traditional/   # 로우레벨(고통) 버전 — 커리큘럼의 "Before" 코드
    improved/      # 하이레벨(해결) 버전 — Spring이 제공하는 추상화 적용
docs/
    PART08_IOC_DI.md   # 학습 노트: 빈 등록 로그, 프록시 클래스 확인, SQL 로그 분석 등
```

## 실행 / 확인 도구

```bash
./gradlew bootRun                          # 앱 실행
./gradlew bootRun --args='--debug'         # 자동 설정·빈 등록 로그 확인 (PART 8)
./gradlew test

# 등록된 핸들러 매핑 확인 (PART 17) — actuator 추가 후
curl localhost:8080/actuator/mappings

# H2 콘솔 (PART 10/14 SQL 직접 확인)
# http://localhost:8080/h2-console  (jdbc:h2:mem:study)
```

## 진행 상황

- [x] PART 8  객체 설계의 진화 → IoC/DI (8.1~8.6 완료, s01~s06 소단원별)
- [x] PART 9  테스트와 웹 인프라 (9.1 JUnit, 9.2 웹 인프라 — 소단원별)
- [x] PART 10 DB 접근의 진화 (10.1~10.5 완료, s01~s05 소단원별)
- [x] PART 11 ORM/JPA와 트랜잭션 추상화 (11.1~11.5 완료, s01~s05 소단원별)
- [ ] PART 12 프록시의 진화와 Spring AOP
- [ ] PART 13 트랜잭션 심화
- [ ] PART 14 JPA 심화
- [ ] PART 15~16 데이터베이스 (이론/운영)
- [ ] PART 17 Spring MVC 내부와 REST API
- [ ] PART 18 분산 시스템·캐싱·메시징·MSA
- [ ] PART 19 Spring Security
- [ ] PART 20 테스트 심화
- [ ] PART 21 HTTP·네트워크·DevOps·Observability
