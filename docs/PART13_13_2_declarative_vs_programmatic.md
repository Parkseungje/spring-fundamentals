# PART 13 — 트랜잭션 심화: 13.2 선언적 vs 프로그래밍 + 프록시 도입 전후

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.2 선언적 vs 프로그래밍 트랜잭션**을 다룬다.
> 트랜잭션을 거는 두 방식(@Transactional vs TransactionTemplate)을 비교하고, '프록시 도입 전후'로 트랜잭션
> 코드가 비즈니스 로직에서 어떻게 분리되는지 본다. PART 11.5의 수동 트랜잭션이 PART 12 프록시로 사라지는 지점.

---

## 0. 들어가기 전에 — 핵심 용어
- **트랜잭션(transaction)**: 여러 DB 작업을 '모두 성공 아니면 모두 취소(원자성)'로 묶는 단위(PART 10.4).
- **선언적 트랜잭션**: `@Transactional` 어노테이션으로 트랜잭션 경계를 '선언'하는 방식(프록시가 처리).
- **프로그래밍 트랜잭션**: `TransactionTemplate`/직접 호출로 트랜잭션을 코드로 '직접' 제어하는 방식.
- **PlatformTransactionManager**: 트랜잭션 시작/커밋/롤백을 추상화한 스프링 표준 인터페이스(PART 11.5).
- **TransactionTemplate**: 프로그래밍 방식용. execute(콜백) 안을 트랜잭션으로 감싼다(12.3 템플릿 콜백).
- **롤백(rollback)**: 트랜잭션 중 문제가 생겨 모든 변경을 취소하고 이전 상태로 되돌리는 것.

한 줄 그림: **트랜잭션을 거는 법은 둘 — 프로그래밍(TransactionTemplate, 세밀하지만 기술 코드가 비즈니스에
섞임)과 선언적(@Transactional, 한 줄로 프록시가 처리해 본문은 순수 비즈니스). 프록시 도입으로 수동 commit/
rollback 코드가 사라지는 게 핵심이며, 그래서 선언적이 거의 표준이다.**

---

## 1. 학습 내용

### 1-1. 프로그래밍 방식 — TransactionTemplate
트랜잭션을 코드로 직접 제어한다. `execute(콜백)` 안의 코드를 트랜잭션으로 감싸고, 콜백이 정상 종료하면
커밋, 예외/`setRollbackOnly()`면 롤백한다(12.3 템플릿 콜백 패턴의 실제 사례 — XxxTemplate 시리즈).
```java
txTemplate.execute(status -> {
    dao.addBalance("A", -300);      // 비즈니스
    if (오류) throw new RuntimeException(...); // -> 롤백
    dao.addBalance("B", 300);       // 비즈니스
    return null;
});
```
- 장점: 트랜잭션 경계를 세밀하게 제어(부분 커밋, 조건부 롤백 등).
- 단점: `execute(콜백)` 구조가 **비즈니스 로직과 뒤섞인다**(기술-비즈니스 강결합). 메서드마다 반복.
- 반환값이 없으면 `executeWithoutResult(status -> {...})`로 더 간결히 쓸 수 있다(`return null` 불필요).

#### ★ 프로그래밍 방식만의 능력 — setRollbackOnly()로 '예외 없이' 롤백
선언적(@Transactional)은 **예외를 던져야** 롤백된다. 그런데 "예외는 안 던지지만(정상 흐름 유지) 이 작업은
취소"하고 싶을 때가 있다(검증 실패·비즈니스 규칙 위반 등). 프로그래밍 방식은 콜백에서 `status.setRollbackOnly()`로
**롤백을 예약**해두면, 예외 없이도 트랜잭션이 롤백된다.
```java
txTemplate.execute(status -> {
    dao.addBalance("A", -300);
    if (검증실패) status.setRollbackOnly();  // 예외 안 던지고 '롤백 예약'
    return null;                             // 정상 종료해도 rollbackOnly라 롤백됨
});
```
- 실측: setRollbackOnly 호출 시 예외 없이도 A가 원복(1000), 호출 안 하면 커밋(700). 이것이 "프로그래밍 방식을
  굳이 왜 쓰나"의 실제 답 중 하나(세밀한 조건부 롤백).
- 롤백 정책: TransactionTemplate도 **자동 롤백은 언체크 예외만**(@Transactional과 동일). 예외 없이 롤백하려면 위 setRollbackOnly.

### 1-2. 선언적 방식 — @Transactional
메서드에 `@Transactional`만 붙이면, 트랜잭션 시작/커밋/롤백을 **프록시**가 대신 처리한다(PART 12의 자동
프록시 생성기 + 트랜잭션 어드바이저). 서비스 본문엔 트랜잭션 코드 없이 **순수 비즈니스 로직만** 남는다.
```java
@Transactional                       // 트랜잭션 경계는 프록시가 담당
public void transfer(String from, String to, int amount) {
    dao.addBalance(from, -amount);   // 순수 비즈니스
    if (오류) throw new RuntimeException(...); // 프록시가 롤백
    dao.addBalance(to, amount);
}
```
- 실측에서 서비스 빈의 실제 클래스가 `...TransferService$$SpringCGLIB$$0`(프록시)였다. → @Transactional이
  프록시 위에서 동작함을 확인.
- 예외 시 프록시가 롤백해 잔액이 원복됐다(A=1000 유지). execute 보일러플레이트가 사라졌다.
> ★ 기본 롤백 정책: @Transactional은 **언체크 예외(RuntimeException/Error)에서만 롤백**한다. 체크 예외는
> 기본적으로 커밋된다(함정). PART 11.5에서 본 그 정책이며, 롤백 대상은 `rollbackFor`로 바꿀 수 있다.

### 1-3. 프록시 '도입 전 vs 후' — 트랜잭션 코드의 분리
**도입 전**(PART 11.5 수동 코드): PlatformTransactionManager로 직접 getTransaction/commit/rollback + try-catch.
```java
TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition()); // 기술
try {
    dao.addBalance(from, -amount);   // 비즈니스 (기술 코드 한가운데 파묻힘)
    if (오류) throw ...;
    dao.addBalance(to, amount);
    txManager.commit(status);        // 기술
} catch (RuntimeException e) {
    txManager.rollback(status);      // 기술
    throw e;
}
```
**도입 후**: `@Transactional` 한 줄. 위 기술 코드를 프록시가 전부 가져간다.
- 두 방식은 **동작이 같다**(실측: 둘 다 예외 시 롤백 → A=1000). 그러나 코드 모습이 완전히 다르다.
- 도입 전 서비스는 평범한 객체(`BeforeProxyService`), 도입 후 서비스는 프록시(`...SpringCGLIB...`)였다.
- 이게 가능한 건 PART 12의 **빈 후처리기 + AnnotationAwareAspectJAutoProxyCreator**(12.8)가 @Transactional
  메서드를 가진 빈을 트랜잭션 프록시로 바꿔치기하기 때문. @EnableTransactionManagement(13.1의 @Import 기반)가 이를 켠다.

#### PlatformTransactionManager 구현체 — 기술에 맞는 게 자동 선택된다
선언적이든 프로그래밍이든, 실제 트랜잭션은 `PlatformTransactionManager` 구현체가 처리한다. 어떤 기술을 쓰느냐에 따라 구현체가 다르다.

| 데이터 접근 기술 | 트랜잭션 매니저 구현체 |
|---|---|
| JDBC / MyBatis / JdbcTemplate | `DataSourceTransactionManager` |
| JPA(Hibernate) | `JpaTransactionManager` |
| (여러 자원 묶기, 분산) | `JtaTransactionManager` |

- 이 단원의 `TxConfig`는 JdbcTemplate을 쓰므로 **`DataSourceTransactionManager`**를 등록했다.
- Spring Boot는 보통 의존성에 맞춰 **자동 구성**한다(data-jpa면 JpaTransactionManager, jdbc면 DataSourceTransactionManager).
- → PART 14(JPA)에선 `JpaTransactionManager`가 쓰이지만, 우리가 코드에서 `@Transactional`을 쓰는 방식은 동일하다(추상화 덕분, PART 10.3 DataSource와 같은 사상).

### 1-4. 무엇을 쓰나 — 선언적이 거의 표준
- 선언적(@Transactional)이 압도적으로 많이 쓰인다(간결·관심사 분리). 단 함정이 많다(내부 호출 13.3,
  @PostConstruct 13.4, 체크 예외 롤백 정책, readOnly 등) — PART 13의 나머지가 이 함정들을 판다.
- 프로그래밍 방식은 "한 메서드 안에서 트랜잭션을 여러 번 쪼개거나 매우 세밀히 제어"해야 하는 드문 경우에 쓴다.

---

## 2. 실습으로 확인하기

> - **가설**: ①TransactionTemplate(프로그래밍)으로 예외 시 롤백된다(단 기술 코드가 비즈니스에 섞임).
>   ②@Transactional(선언적)도 롤백되며 서비스 본문은 순수 비즈니스(빈은 프록시). ③프록시 전(수동)/후
>   (@Transactional)는 동작은 같고 코드 분리만 다르다.

### 코드 (`com.study.part13_tx.s02_declarative_vs_programmatic`)
- `TxConfig`(H2 DataSource/JdbcTemplate/txManager/TransactionTemplate, @EnableTransactionManagement), `AccountDao`.
- `Example1_Programmatic` / `Example2_Declarative` / `Example3_ProxyBeforeAfter`.
- `Example4_SetRollbackOnly` — 예외 없이 setRollbackOnly()로 롤백 vs 표시 없으면 커밋.
- (각 예제는 13.1의 `@Import(TxConfig.class)`로 공용 설정을 재사용한다.)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s02_declarative_vs_programmatic.Example1_Programmatic
./gradlew runStage -Pmain=com.study.part13_tx.s02_declarative_vs_programmatic.Example2_Declarative
./gradlew runStage -Pmain=com.study.part13_tx.s02_declarative_vs_programmatic.Example3_ProxyBeforeAfter
./gradlew runStage -Pmain=com.study.part13_tx.s02_declarative_vs_programmatic.Example4_SetRollbackOnly
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (프로그래밍): 예외 → 롤백 → A 원복.
```
A 출금 완료(아직 커밋 전) -> 예외 -> 이체 후: A=1000 B=1000
```
예제2 (선언적): 빈이 프록시, 예외 → 롤백.
```
서비스 실제 클래스 = ...TransferService$$SpringCGLIB$$0 (프록시)
이체 후: A=1000 B=1000
```
예제3 (프록시 전/후): 동작 동일, 코드 모습만 다름.
```
[전] 실제 클래스 = BeforeProxyService (프록시 아님) -> 롤백 -> A=1000
[후] 실제 클래스 = ...AfterProxyService$$SpringCGLIB$$0 (프록시) -> 롤백 -> A=1000
```
- 세 방식 모두 원자성(롤백)은 같고, 선언적/프록시 도입이 '트랜잭션 코드를 비즈니스에서 분리'함이 확인됐다. ✅

예제4 (setRollbackOnly):
```
(A) 출금 후 setRollbackOnly() -> 예외 없이 롤백 -> A=1000 (원복)
(B) 출금 후 표시 없음          -> 커밋          -> A=700  (반영)
```
- 예외를 안 던졌는데도 setRollbackOnly로 롤백됐다. ✅ → 선언적(예외 필요)과 다른, 프로그래밍 방식만의 세밀 제어.

---

## 3. 자기 점검

- **Q. 선언적 트랜잭션과 프로그래밍 트랜잭션의 차이는?**
  - 내 답: 선언적(@Transactional)은 어노테이션 한 줄로 프록시가 트랜잭션을 처리해 본문이 순수 비즈니스만
    남는다. 프로그래밍(TransactionTemplate)은 코드로 세밀 제어가 가능하나 기술 코드가 비즈니스에 섞인다.
    실무는 선언적이 거의 표준.

- **Q. @Transactional은 어떻게 트랜잭션을 거나?**
  - 내 답: 프록시(자동 프록시 생성기+트랜잭션 어드바이저, 12.8)가 메서드 호출을 가로채 시작/커밋/롤백을
    처리한다. 빈이 SpringCGLIB 프록시로 바뀐다. @EnableTransactionManagement(@Import 기반)가 이를 켠다.

- **Q. 프록시 도입 전후로 서비스 코드는 어떻게 달라지나?**
  - 내 답: 전에는 getTransaction/commit/rollback+try-catch가 비즈니스에 뒤섞였고(PART 11.5), 후에는
    @Transactional 한 줄만 남고 기술 코드는 프록시가 가져간다. 동작은 같고 관심사 분리만 달라진다.

- **Q. @Transactional의 기본 롤백 정책 함정은?**
  - 내 답: 언체크 예외(RuntimeException/Error)에서만 롤백하고 체크 예외는 기본 커밋된다. rollbackFor로 조정.
    (PART 11.5에서 다룬 함정.)

- **Q. 예외를 안 던지고 롤백하려면? (프로그래밍 방식만의 능력)**
  - 내 답: TransactionTemplate 콜백에서 status.setRollbackOnly()로 롤백을 예약하면, 정상 종료해도 롤백된다.
    선언적(@Transactional)은 예외를 던져야 롤백되므로, 검증 실패 등 '예외 없는 조건부 롤백'은 프로그래밍 방식이 유리.

- **Q. 트랜잭션 매니저 구현체는 기술마다 다른가?**
  - 내 답: 그렇다. JDBC/MyBatis=DataSourceTransactionManager, JPA=JpaTransactionManager, 분산=JtaTransactionManager.
    Spring Boot가 의존성에 맞춰 자동 구성한다. 우리가 @Transactional을 쓰는 방식은 구현체와 무관하게 동일(추상화).
