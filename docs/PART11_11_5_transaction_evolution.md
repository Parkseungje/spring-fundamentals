# PART 11 — ORM/JPA와 트랜잭션 추상화: 11.5 트랜잭션 추상화의 진화

> 이 문서는 커리큘럼 PART 11의 마지막 소단원 **11.5 트랜잭션 추상화의 진화**를 다룬다.
> 수동 트랜잭션(10.4) → PlatformTransactionManager → `@Transactional`(선언적, AOP)로 가는 마지막 진화를
> 본다. 여기서 처음 만나는 '프록시'가 PART 12(AOP)로 이어진다.

---

## 0. 들어가기 전에 — 핵심 용어
- **수동 트랜잭션**: `setAutoCommit(false)`/`commit()`/`rollback()`/`close()`를 직접 호출하는 방식(10.4).
- **PlatformTransactionManager**: 트랜잭션을 추상화한 Spring 인터페이스(구현체를 갈아끼움 — DataSource와 같은 사상).
- **@Transactional**: 메서드에 붙이면 트랜잭션 시작·커밋·롤백을 자동 처리하는 어노테이션(선언적 트랜잭션).
- **프록시(proxy)**: 원본 객체의 '대리자'. 호출 전후에 트랜잭션 시작/커밋·롤백을 끼워 넣는다. @Transactional 빈은 실제로 프록시가 등록된다.
- **AOP(Aspect-Oriented Programming)**: 공통 관심사(트랜잭션 등)를 핵심 로직과 분리해 끼워 넣는 기법(PART 12).
- **변경 감지(dirty checking)**: 트랜잭션 안에서 조회한 엔티티의 필드를 바꾸면 커밋 시 UPDATE가 자동 반영.
- **전파(propagation)**: 이미 트랜잭션이 있을 때 어떻게 행동할지(REQUIRED/REQUIRES_NEW/NESTED).

한 줄 그림: **수동 트랜잭션의 보일러플레이트를 PlatformTransactionManager로 추상화하고, 다시 프록시(AOP)로
감싸 `@Transactional` 한 줄로 줄인다. 메서드 시작=트랜잭션 시작, 정상=커밋, 런타임 예외=롤백을 자동으로.**

---

## 1. 학습 내용 — 세 단계 진화

### ① 수동 트랜잭션의 한계 (10.4)
원자성을 보장하려면 출금·입금이 **같은 Connection**이어야 한다. 그래서 Connection을 비즈니스 로직에
파라미터로 넘기게 되고(추상화 깨짐), 메서드마다 try/commit/rollback/close가 반복된다.
```java
conn.setAutoCommit(false);
try { ...; conn.commit(); }
catch (Exception e) { conn.rollback(); throw e; }
finally { conn.close(); }
```
3대 함정: 트랜잭션 누수(반복 코드), 예외 누수(SQLException 전파), JDBC 반복.

### ② PlatformTransactionManager (인터페이스 추상화)
PART 10의 DataSource와 같은 사상 — 트랜잭션 처리를 인터페이스로 추상화한다.
```java
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition def);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}
```
- 구현체: **DataSourceTransactionManager**(JDBC/JdbcTemplate/MyBatis), **JpaTransactionManager**(JPA),
  HibernateTransactionManager. Spring Boot가 의존성에 맞춰 자동 구성한다.
- Connection 매개변수는 사라졌지만, getTransaction/commit/rollback 보일러플레이트는 여전히 남는다.

### ③ @Transactional (선언적 트랜잭션, AOP)
보일러플레이트마저 **프록시**가 대신 끼워 넣는다. 개발자는 어노테이션만 붙인다.
```java
@Transactional
public void transfer(Long fromId, Long toId, int amount) {
    Account from = repo.findById(fromId).orElseThrow();
    Account to   = repo.findById(toId).orElseThrow();
    from.withdraw(amount);   // 변경 감지(dirty checking) -> 커밋 시 UPDATE 자동
    to.deposit(amount);
}   // 트랜잭션 시작·커밋·롤백·Connection 관리 전부 자동
```
- **프록시 원리**: @Transactional 빈은 Spring이 대리자(프록시)로 감싸 등록한다. 외부에서 transfer()를
  호출하면 → 프록시가 '트랜잭션 시작' → 진짜 메서드 실행 → 정상이면 '커밋', 런타임 예외면 '롤백'.
  이 프록시의 정체가 **PART 12 AOP**다.

### ★ @Transactional의 5가지 함정
1. **private 메서드 ❌** — 프록시가 외부 호출만 가로채므로 private에는 트랜잭션이 안 걸린다(public에 붙여야).
2. **Self-invocation** — 같은 클래스 안에서 `this.inner()`로 호출하면 프록시를 안 거쳐 트랜잭션이 안 걸린다.
   (다른 빈을 통해 호출해야 프록시를 탄다.)
3. **기본 롤백은 RuntimeException(unchecked)만** — 체크 예외는 롤백 안 됨. `@Transactional(rollbackFor = Exception.class)`로 지정.
4. **전파 옵션** — `REQUIRED`(기본: 있으면 참여, 없으면 새로), `REQUIRES_NEW`(항상 새 트랜잭션), `NESTED`(중첩/Savepoint).
5. **`readOnly = true`** — 읽기 전용 메서드에 붙이면 영속성 컨텍스트 최적화(플러시 생략 등)로 성능 ↑.

### 집약
PART 8 OOP 원칙(템플릿 메소드/전략/OCP/DI) + PART 10 PlatformTransactionManager + 프록시(AOP) →
**어노테이션 한 줄(@Transactional)**. 이 "프록시"가 곧 PART 12의 주제다.

---

## 2. 실습으로 확인하기

> - **가설 1**: @Transactional transfer는 출금+입금이 함께 커밋된다(A=700, B=1300).
> - **가설 2**: 이체 중 RuntimeException이 나면 @Transactional이 자동 롤백 → 출금도 취소(A·B 그대로).

### 코드 (`com.study.part11_jpa.s05_transaction`)
- `Account`(엔티티, withdraw/deposit) + `AccountRepository`.
- `AccountService` — `@Transactional transfer`(정상)/`transferWithError`(예외→롤백)/`balanceOf`(readOnly).
- 테스트 `TransactionalTest`(@SpringBootTest, 풀 컨텍스트라 서비스가 프록시 빈) — 커밋/롤백 검증.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew test --tests "com.study.part11_jpa.s05_transaction.*"
```

### 실행 결과 — 가설과 실제 비교
- `BUILD SUCCESSFUL` — 통과. ✅
  - transferCommits: 이체 후 A=700, B=1300(함께 커밋).
  - transferRollsBackOnError: 예외 후 A=1000, B=1000(자동 롤백 → 출금 취소). **수동 rollback 코드 없이** 어노테이션이 처리.

---

## 3. 자기 점검

- **Q. @Transactional은 내부적으로 어떻게 트랜잭션을 거나?**
  - 내 답: 프록시(대리자)가 메서드 호출 전후를 가로채 트랜잭션 시작/커밋/롤백을 끼워 넣는다(AOP). @Transactional
    빈은 실제로 프록시 객체가 등록된다.

- **Q. 기본적으로 어떤 예외에서 롤백되나? 체크 예외는?**
  - 내 답: 기본은 RuntimeException(unchecked)·Error에서 롤백. 체크 예외는 롤백 안 되므로 `rollbackFor = Exception.class` 지정 필요.

- **Q. self-invocation 함정이란?**
  - 내 답: 같은 클래스 안에서 this로 @Transactional 메서드를 호출하면 프록시를 거치지 않아 트랜잭션이 안
    걸린다. 외부(다른 빈)를 통해 호출해야 프록시가 적용된다. (private 메서드도 같은 이유로 안 걸림.)

- **Q. PlatformTransactionManager와 @Transactional의 관계는?**
  - 내 답: @Transactional은 내부적으로 PlatformTransactionManager(JpaTransactionManager 등)를 사용해 트랜잭션을
    시작/커밋/롤백한다. 프록시가 그 호출을 대신해 준다. (PART 10 DataSource와 같은 인터페이스 추상화 사상.)
