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
   (다른 빈을 통해 호출해야 프록시를 탄다.) → **상세 실증은 PART 13.3**. (짝 함정: @PostConstruct는 13.4)
3. **기본 롤백은 RuntimeException(unchecked)만** — 체크 예외는 롤백 안 됨. `@Transactional(rollbackFor = Exception.class)`로 지정.
4. **전파 옵션** — `REQUIRED`(기본: 있으면 참여, 없으면 새로), `REQUIRES_NEW`(항상 새 트랜잭션), `NESTED`(중첩/Savepoint).
   → **상세 실증은 PART 13.5**(내부 롤백 시 UnexpectedRollbackException 함정 포함).
5. **`readOnly = true`** — 읽기 전용 메서드에 붙이면 영속성 컨텍스트 최적화(플러시 생략 등)로 성능 ↑.

### @Transactional 주요 속성 한눈에
함정 5개에 흩어진 옵션을 한 표로 정리한다(실무 레퍼런스).

| 속성 | 의미 | 기본값 | 비고 |
|---|---|---|---|
| `propagation` | 진행 중 트랜잭션이 있을 때 동작 | REQUIRED | REQUIRES_NEW/NESTED 등 (PART 13.5) |
| `isolation` | 격리 수준 | DEFAULT(DB 기본) | READ_COMMITTED/REPEATABLE_READ 등 (PART 13.6, 10.4) |
| `readOnly` | 읽기 전용 최적화 | false | 조회 메서드는 true 권장(플러시 생략·스냅샷 보관 안 함) |
| `timeout` | 트랜잭션 제한 시간(초) | -1(무제한) | 초과 시 롤백 + 예외. 오래 걸리는 트랜잭션 방어 |
| `rollbackFor` | 추가로 롤백할 예외 | (RuntimeException/Error) | 예: `rollbackFor = Exception.class`(체크 예외도 롤백) |
| `noRollbackFor` | 롤백 '제외'할 예외 | 없음 | 예: 특정 RuntimeException은 커밋하고 싶을 때 |

- 롤백 정책 정리: **기본은 unchecked(RuntimeException/Error)만 롤백**. `rollbackFor`로 대상을 넓히고,
  `noRollbackFor`로 특정 예외를 롤백에서 빼낸다.

### 클래스 레벨 @Transactional
어노테이션은 메서드뿐 아니라 **클래스에도** 붙일 수 있다.
```java
@Service
@Transactional(readOnly = true)        // 클래스 기본: 모든 public 메서드에 적용
public class AccountService {
    public int balanceOf(Long id) { ... }          // readOnly = true (클래스 설정 상속)

    @Transactional                                  // 메서드 레벨이 '우선' -> 쓰기 가능(readOnly=false)
    public void transfer(...) { ... }
}
```
- 클래스에 붙이면 그 안 **모든 public 메서드**에 적용되고, **메서드 레벨 설정이 우선**한다.
- 흔한 패턴: 클래스에 `@Transactional(readOnly = true)`로 기본을 읽기 전용으로 깔고, 쓰기 메서드에만
  `@Transactional`을 따로 붙여 덮어쓴다(조회 최적화 + 쓰기는 명시).
- (private/내부 호출엔 여전히 안 걸린다 — 클래스에 붙여도 프록시 원리는 동일.)

### ★ 트랜잭션 경계 = 영속성 컨텍스트 경계
@Transactional이 단순히 commit/rollback만 자동화하는 게 아니다. **트랜잭션의 시작~끝이 곧 영속성 컨텍스트의
생존 범위**다(11.3). 그래서 변경 감지·1차 캐시·지연 로딩이 모두 이 안에서만 동작한다.
```
@Transactional 시작 ──► [영속성 컨텍스트 생성] ── 조회/변경 ── [커밋 시 변경 감지로 UPDATE flush] ──► 끝(컨텍스트 닫힘)
```
- 그래서 변경 감지(위 transfer의 withdraw/deposit이 save 없이 반영)가 **커밋 시점**에 일어난다.
- 트랜잭션이 끝나면 컨텍스트가 닫혀, 그 밖에서 지연 로딩을 시도하면 예외가 난다(상세는 PART 14).

### ★ 변경 감지는 왜 'Update만' 자동인가 (CRUD 비교)
"필드만 바꿔도 반영된다"는 변경 감지는 **CRUD 중 Update 전용** 메커니즘이다. Insert·Delete는 메서드를
명시적으로 호출해야 한다.

| | 어떻게 트리거되나 | 자동? |
|---|---|---|
| Create(insert) | `persist()`/`save()`를 호출해야 함 | ❌ 명시적 |
| Read(select) | `findById()`/JPQL을 호출해야 함 | ❌ 명시적 |
| Update | 영속 엔티티의 필드를 바꾸면 커밋 시 자동 | ✅ 변경 감지 |
| Delete | `remove()`/`delete()`를 호출해야 함 | ❌ 명시적 |

#### 원리 — '스냅샷 비교'
JPA가 "필드가 바뀐 것"을 아는 방법이 핵심이다.
```
1) 엔티티가 '영속 상태'가 되는 순간(조회 or persist)
   -> 영속성 컨텍스트가 1차 캐시에 엔티티를 보관 + 동시에 '스냅샷'(그 시점 필드값 복사본)을 따로 저장
2) 코드에서 entity.changePrice(9900) -> 객체 필드만 바뀜(아직 DB는 모름)
3) 커밋 -> flush 시점에 [현재 값] vs [1)의 스냅샷]을 필드별로 비교(dirty check)
   -> 다른 필드가 있으면 UPDATE SQL 자동 생성 -> DB 전송
4) commit
```
"읽을 때 원본 사진을 찍어두고, 커밋할 때 현재와 대조해 달라진 부분만 UPDATE"를 만드는 것이다.

#### 왜 Insert·Delete는 자동이 아닌가 (설계 의도)
- **Update = 이미 관리 중인 대상의 '상태 변화'** → 의도가 분명("이 데이터를 이렇게 바꿔라")해 자동으로 잡아도 안전.
- **Insert/Delete = 관리 대상에 '넣고 빼는 행위'(생명주기 전이)** → 명시적 의사결정이 필요하다.
  - `new Account()`를 보고 JPA가 멋대로 insert하면 의도치 않은 저장 위험 → `persist()`로 "넣겠다"를 명시.
  - 참조를 안 한다고 멋대로 delete하면 큰 사고 → `remove()`로 "지우겠다"를 명시.
- 비유: 영속성 컨텍스트를 **자바 컬렉션처럼** 다루는 설계다. 컬렉션에서 꺼낸 객체의 필드를 바꾸면 반영되듯
  (Update 자동), 컬렉션에 **add/remove는 명시적으로** 하듯 persist/remove도 명시한다.

> ★ 헷갈리는 점 2가지
> - **영속 상태에서만 자동**: 변경 감지는 '영속성 컨텍스트가 관리 중인(영속)' 엔티티에만 작동한다. `new`로
>   막 만든(비영속) 객체나, 트랜잭션이 끝나 분리된(준영속) 객체는 필드를 바꿔도 UPDATE가 안 나간다.
> - **Spring Data `save()` 혼동**: update할 때 습관적으로 `save()`를 부르는 경우가 많지만, **영속 엔티티는
>   `save()` 없이 필드만 바꿔도 반영**된다(변경 감지). `save()`는 비영속을 insert하거나 준영속을 병합(merge)할 때 의미가 있다.
> (생명주기 4상태 — 비영속/영속/준영속/삭제 — 와 쓰기 지연 SQL 저장소는 PART 14에서 깊이 다룬다.)

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

- **Q. @Transactional 주요 속성을 들면? (rollbackFor vs noRollbackFor, timeout)**
  - 내 답: propagation/isolation/readOnly/timeout/rollbackFor/noRollbackFor. 기본은 unchecked만 롤백 →
    rollbackFor로 대상 확대(체크 예외 포함), noRollbackFor로 특정 예외 제외. timeout은 제한 시간(초) 초과 시 롤백.
    readOnly=true는 조회 최적화(플러시 생략).

- **Q. 클래스에 @Transactional을 붙이면?**
  - 내 답: 그 안 모든 public 메서드에 적용되고, 메서드 레벨 설정이 우선한다. 흔히 클래스에 readOnly=true를
    깔고 쓰기 메서드만 @Transactional로 덮어쓴다. (private/내부 호출엔 여전히 안 걸림.)

- **Q. 변경 감지가 왜 트랜잭션 안에서만 동작하나?**
  - 내 답: 트랜잭션 경계 = 영속성 컨텍스트 생존 범위라서. @Transactional이 시작될 때 컨텍스트가 만들어지고,
    커밋 시점에 변경 감지로 UPDATE가 flush된다. 트랜잭션이 끝나면 컨텍스트가 닫혀 지연 로딩도 불가(PART 14).

- **Q. 변경 감지는 왜 CRUD 중 Update만 자동인가?**
  - 내 답: 변경 감지는 Update 전용 메커니즘이다. 영속 엔티티가 조회/영속화될 때 떠둔 '스냅샷'과 커밋 시점의
    현재 값을 비교해 달라진 필드로 UPDATE를 만든다. Insert/Delete는 '관리 대상에 넣고 빼는 생명주기 전이'라
    명시적 의도가 필요해 persist()/remove()를 호출해야 한다(자바 컬렉션의 add/remove처럼). Update는 '관리 중
    대상의 상태 변화'라 의도가 분명해 자동으로 잡아도 안전. 단 영속 상태에서만 작동한다.
