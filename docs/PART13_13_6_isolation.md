# PART 13 — 트랜잭션 심화: 13.6 격리 수준 (Spring 선언 방식 + 전파와의 상호작용)

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.6 격리 수준**을 다룬다.
> 격리 수준의 '이론'(3대 이상현상·격리 매트릭스·MVCC)은 이미 **PART 10.4**에서 코드로 충분히 다뤘다
> (`Example3_ReadPhenomena`). 그래서 13.6은 중복을 피하고, 10.4에 없던 **Spring 고유의 두 가지**에 집중한다:
> ① `@Transactional(isolation=...)` 선언적 지정 ② 격리 수준과 전파(13.5)의 상호작용.

---

## 0. 들어가기 전에 — 핵심 용어
- **격리 수준(isolation level)**: 동시 트랜잭션이 서로를 얼마나 차단하나(READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE). 이론은 10.4.
- **선언적 지정**: `@Transactional(isolation = Isolation.XXX)` — 코드(JDBC) 대신 어노테이션으로 격리를 정함.
- **Isolation.DEFAULT**: 격리를 따로 지정 안 함 → DB의 기본 격리 수준을 사용(H2/PostgreSQL/Oracle=READ COMMITTED, MySQL=REPEATABLE READ).
- **전파(propagation)**: 진행 중 트랜잭션에 참여할지/새로 시작할지(13.5). REQUIRED=참여, REQUIRES_NEW=새로.

한 줄 그림: **Spring에선 `@Transactional(isolation=...)`로 격리를 선언적으로 정한다. 그런데 격리는
'트랜잭션이 시작될 때' 정해지므로, REQUIRED로 기존 트랜잭션에 '참여'하면 내가 선언한 격리는 무시되고
외부 격리를 따른다. 격리를 바꾸려면 REQUIRES_NEW로 '새 트랜잭션'을 시작해야 한다.**

---

## 1. 학습 내용

### 1-1. (복습은 10.4) 이론은 거기서 — 여기서는 Spring 적용
3대 이상현상(Dirty / Non-Repeatable / Phantom)과 격리 수준 × 이상현상 매트릭스, MVCC는 PART 10.4에 정리돼
있고 `Example3_ReadPhenomena`로 격리 수준별 재현까지 했다. (요약: 위 수준일수록 빠르지만 이상현상에 취약,
아래일수록 안전하지만 느리다.) 13.6은 그 위에 **"Spring으로 어떻게 지정하고, 전파와 어떻게 얽히나"** 만 얹는다.

### 1-2. 선언적 격리 지정 — @Transactional(isolation=...)
10.4에선 `conn.setTransactionIsolation()`(JDBC 저수준)으로 격리를 바꿨다. 실무 Spring에선 어노테이션으로 정한다.
```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void doWork() { ... }
```
- 지정 안 하면 **`Isolation.DEFAULT`** → DB 기본 격리 사용(H2=READ COMMITTED).
- 명시하면 그 트랜잭션이 그 격리로 시작된다. 확인은 `TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()`
  (현재 트랜잭션에 설정된 격리; 미지정이면 null).
- 실측: useDefault → DEFAULT(null), readCommitted → 2, repeatableRead → 4. 선언한 대로 잡힌다.

### 1-3. ★ 격리 × 전파 — "격리는 트랜잭션이 시작될 때 정해진다"
이게 13.6의 핵심이자 13.5(전파)와 만나는 지점이다. 직관적으로 "내부 메서드에 isolation을 지정하면 그게
적용되겠지" 싶지만, **전파가 REQUIRED(참여)면 그렇지 않다.**

- **REQUIRED로 참여**: 새 트랜잭션을 시작하는 게 아니라 **외부 트랜잭션을 그대로 쓴다**. 격리는 트랜잭션이
  '시작될 때' 한 번 정해지는 속성이라, 이미 시작된 외부 트랜잭션의 격리를 따른다 → **내부가 선언한 격리는 무시**.
  ```
  outer(READ_COMMITTED) -> inner(REQUIRED, 선언 SERIALIZABLE)
    => inner 실제 격리 = READ_COMMITTED  (SERIALIZABLE 무시됨)
  ```
- **REQUIRES_NEW로 새로 시작**: 외부를 보류하고 **새 물리 트랜잭션**을 시작하므로, 그 시작 시점에 자기가
  선언한 격리가 그대로 적용된다.
  ```
  outer(READ_COMMITTED) -> inner(REQUIRES_NEW, 선언 SERIALIZABLE)
    => inner 실제 격리 = SERIALIZABLE  (자기 격리 적용)
  ```
- 정리: **"격리를 바꾸고 싶으면 새 트랜잭션을 시작해야 한다(REQUIRES_NEW). 기존 트랜잭션에 참여(REQUIRED)하면
  격리를 못 바꾼다."**

> ★ 헷갈리는 지점 — 참여 중 다른 격리를 선언하면 에러가 나야 하지 않나? 스프링은 기본적으로 참여
> 트랜잭션의 속성 불일치를 '검증하지 않아' **조용히 무시**한다(그래서 위처럼 외부 격리를 따른다). 트랜잭션
> 매니저의 `validateExistingTransaction=true`로 켜면 격리/읽기전용 불일치 시 `IllegalTransactionStateException`을
> 던지게 만들 수 있다. 기본값(false)에선 "무시"라는 점을 알아 둬야 디버깅 때 안 헤맨다.

---

## 2. 실습으로 확인하기

> - **가설 1**: @Transactional(isolation=...)로 격리가 선언대로 잡힌다(미지정=DEFAULT).
> - **가설 2**: REQUIRED로 참여하면 내부 선언 격리는 무시되고 외부 격리를 따른다.
> - **가설 3**: REQUIRES_NEW면 새 트랜잭션이라 내부가 선언한 격리가 적용된다.

### 코드 (`com.study.part13_tx.s06_isolation`)
- `IsoUtil`(현재 격리 읽기), `IsoServices`(SimpleIso / InnerIso / OuterIso).
- `Example1_DeclarativeIsolation` / `Example2_IsolationParticipating` / `Example3_IsolationRequiresNew`.
- (트랜잭션 설정은 13.2의 `@Import(TxConfig.class)` 재사용.)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s06_isolation.Example1_DeclarativeIsolation
./gradlew runStage -Pmain=com.study.part13_tx.s06_isolation.Example2_IsolationParticipating
./gradlew runStage -Pmain=com.study.part13_tx.s06_isolation.Example3_IsolationRequiresNew
```

### 실행 결과 — 가설과 실제 비교 (실측)
```
예제1 (선언적 지정)
  useDefault()     -> DEFAULT(지정 안 함 -> DB 기본)
  readCommitted()  -> READ_COMMITTED(2)
  repeatableRead() -> REPEATABLE_READ(4)

예제2 (REQUIRED 참여)
  outer 격리 = READ_COMMITTED(2)
  inner(REQUIRED, 선언=SERIALIZABLE) 실제 격리 = READ_COMMITTED(2)   <- 선언 무시, 외부 따름

예제3 (REQUIRES_NEW)
  outer 격리 = READ_COMMITTED(2)
  inner(REQUIRES_NEW, 선언=SERIALIZABLE) 실제 격리 = SERIALIZABLE(8) <- 자기 격리 적용
```
- 선언적 지정 동작 + "참여하면 격리 못 바꿈 / 새 트랜잭션이면 바꿈"이 또렷이 확인됐다. ✅

---

## 3. 자기 점검

- **Q. Spring에서 격리 수준은 어떻게 지정하나?**
  - 내 답: `@Transactional(isolation = Isolation.XXX)`로 선언적으로 지정. 미지정(DEFAULT)이면 DB 기본 격리
    사용. (10.4의 JDBC setTransactionIsolation 저수준 방식의 Spring 표준판.)

- **Q. 내부 메서드에 isolation을 지정했는데 안 먹는 경우는?**
  - 내 답: 전파가 REQUIRED라 외부 트랜잭션에 '참여'할 때. 격리는 트랜잭션 시작 시점에 정해지는데 새로
    시작하는 게 아니라 외부 것을 쓰므로, 내부 선언 격리는 무시되고 외부 격리를 따른다(스프링 기본은 조용히 무시).

- **Q. 그럼 내부에서 격리를 바꾸려면?**
  - 내 답: REQUIRES_NEW로 새 물리 트랜잭션을 시작하면 자기 격리가 적용된다. "격리를 바꾸려면 새 트랜잭션".

- **Q. 격리 수준의 이상현상/매트릭스는 어디서 봤나?**
  - 내 답: PART 10.4(ACID의 I 심화). Dirty/Non-Repeatable/Phantom + 격리×현상 매트릭스 + MVCC +
    Example3_ReadPhenomena로 재현. 13.6은 그 위에 Spring 선언 방식과 전파 상호작용만 얹은 것.
