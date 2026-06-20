# PART 13 — 트랜잭션 심화: 13.5 트랜잭션 전파 (Propagation)

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.5 트랜잭션 전파**를 다룬다.
> "트랜잭션 안에서 또 트랜잭션 메서드를 부르면 어떻게 되나?"를 정하는 규칙이다. 면접 단골인 내부 롤백
> 함정(UnexpectedRollbackException)과 REQUIRED vs REQUIRES_NEW의 차이를 실제 DB 상태로 확인한다.

---

## 0. 들어가기 전에 — 핵심 용어
- **전파(propagation)**: 이미 진행 중인 트랜잭션이 있을 때, 새로 호출된 @Transactional 메서드를 어떻게 처리할지의 규칙.
- **물리 트랜잭션(physical)**: 실제 DB 커넥션 단위의 진짜 트랜잭션(commit/rollback이 실제로 일어나는 단위).
- **논리 트랜잭션(logical)**: @Transactional 메서드 하나하나의 '논리적 경계'. 여러 논리가 하나의 물리에 묶일 수 있다.
- **REQUIRED**: (기본) 진행 중 트랜잭션이 있으면 참여, 없으면 새로 시작.
- **REQUIRES_NEW**: 항상 새 물리 트랜잭션을 시작(외부와 분리).
- **rollbackOnly**: "이 트랜잭션은 롤백되어야 함"이라는 표시. 한 번 켜지면 커밋이 불가능.
- **UnexpectedRollbackException**: 커밋하려 했으나 rollbackOnly가 켜져 있어 강제 롤백되며 던져지는 예외.

한 줄 그림: **REQUIRED는 외부 트랜잭션에 '참여'해 하나의 물리 트랜잭션으로 묶인다 — 내부가 실패하면
전체가 롤백된다. 내부 예외를 try-catch로 삼켜도 rollbackOnly가 켜져 외부 커밋이 UnexpectedRollbackException으로
실패한다. 외부를 살리려면 내부를 REQUIRES_NEW로 '분리'해야 한다.**

---

## 1. 학습 내용

### 1-1. 전파란 — "트랜잭션 안의 트랜잭션"을 어떻게?
서비스 A의 @Transactional 메서드가, 서비스 B의 @Transactional 메서드를 호출하는 일은 흔하다. 이때 B를
"A와 같은 트랜잭션으로 묶을지, 따로 떼어낼지"를 정하는 게 전파다(B의 `@Transactional(propagation=...)`).

### 1-2. REQUIRED(기본) — 외부에 '참여'(하나의 물리 트랜잭션)
- 진행 중인 트랜잭션이 있으면 새로 만들지 않고 **그 트랜잭션에 참여**한다. 외부+내부가 **하나의 물리 트랜잭션**.
- 그래서 내부의 commit/rollback은 '논리적'일 뿐, **실제 물리 commit/rollback은 트랜잭션을 시작한 외부에서만** 일어난다.
- 결과(실측):
  - (A) 내부 정상 → 외부 A-100, 내부 B+100이 **함께 커밋**(A=900, B=1100).
  - (B) 내부 실패(예외 전파) → 같은 물리 트랜잭션이라 **외부 작업까지 전부 롤백**(A=1000, B=1000).
- → "내부가 새 트랜잭션이 아니라 외부에 참여했다"는 증거: 내부 실패가 외부 작업까지 되돌린다.

### 1-3. ★ 내부 롤백의 함정 — UnexpectedRollbackException
가장 헷갈리는 부분. "내부 예외를 try-catch로 잡았으니 외부는 계속 커밋되겠지?"는 **틀렸다.**
```java
@Transactional                    // 외부 (REQUIRED)
public void outer() {
    dao.addBalance("A", -100);
    try {
        inner.addAndFail();       // 내부 REQUIRED, 실패
    } catch (RuntimeException e) {
        // 예외를 삼키고 계속 진행하려 함
    }
    // 메서드 정상 종료 -> 프록시가 커밋 시도 -> ???
}
```
- 내부(REQUIRED)가 실패하면, **같은 물리 트랜잭션을 `rollbackOnly`로 표시**해 버린다. 이 표시는 외부가
  예외를 삼켜도 사라지지 않는다.
- 외부가 정상 종료해 프록시가 커밋을 시도하면, rollbackOnly를 발견하고 → **실제로는 롤백**하면서
  **`UnexpectedRollbackException`** 을 던진다(실측: A=1000, B=1000 둘 다 롤백).
- 본질: **"논리 트랜잭션(내부)이 하나라도 롤백 표시되면, 물리 트랜잭션(전체)은 롤백된다."**
> ★ 13.1(B) vs 13.5 함정의 차이 — 13.1(B)는 예외를 안 잡아 그냥 전파·롤백. 13.5 함정은 예외를 '잡았는데도'
> 결국 롤백 + 예외가 난다는 점이 더 함정스럽다. "내부 트랜잭션 메서드 호출을 try-catch로 감싸면 안전하다"는
> 착각을 깨야 한다.

### 1-4. REQUIRES_NEW — '별도 물리 트랜잭션'으로 분리
외부를 살리고 싶다면 내부를 외부와 **분리된 트랜잭션**으로 만든다.
- REQUIRES_NEW는 진행 중인 외부 트랜잭션을 **잠시 보류**하고 **새 물리 트랜잭션**을 시작한다.
- 내부가 실패하면 **그 새 트랜잭션만 롤백**되고, 외부가 그 예외를 잡으면 외부는 자기 작업을 **그대로 커밋**할 수 있다.
- 결과(실측): A=900(외부 커밋), B=1000(내부만 롤백). UnexpectedRollbackException 없음.
- ⚠️ **주의**: REQUIRES_NEW는 외부+내부가 **동시에 커넥션 2개**를 점유한다. 남용하면 커넥션 풀 고갈(10.2) 위험.

### 1-5. 전파 7옵션과 실무
| 옵션 | 진행 중 트랜잭션이 있을 때 | 없을 때 |
|---|---|---|
| REQUIRED(기본) | 참여 | 새로 시작 |
| REQUIRES_NEW | 보류하고 새로 시작(분리) | 새로 시작 |
| SUPPORTS | 참여 | 트랜잭션 없이 실행 |
| NOT_SUPPORTED | 보류하고 트랜잭션 없이 실행 | 트랜잭션 없이 실행 |
| MANDATORY | 참여 | 예외(반드시 있어야) |
| NEVER | 예외(있으면 안 됨) | 트랜잭션 없이 실행 |
| NESTED | Savepoint로 중첩(부분 롤백 가능, 10.4) | 새로 시작 |

- 실무는 **REQUIRED**(대부분) + **REQUIRES_NEW**(로그·이력처럼 본 작업과 독립적으로 남겨야 할 때) 중심.
- 지정: `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

---

## 2. 실습으로 확인하기

> - **가설 1**: REQUIRED는 외부에 참여 — (A)정상이면 함께 커밋, (B)내부 실패면 외부까지 전부 롤백.
> - **가설 2**: 내부(REQUIRED) 실패를 외부가 try-catch로 삼켜도 커밋 시 UnexpectedRollbackException + 전체 롤백.
> - **가설 3**: REQUIRES_NEW는 별도 트랜잭션 — 내부만 롤백되고 외부는 커밋된다.

### 코드 (`com.study.part13_tx.s05_propagation`)
- `InnerService`(REQUIRED/REQUIRES_NEW 메서드), `OuterService`(4가지 시나리오). 관찰은 A/B 잔액.
- `Example1_Required` / `Example2_UnexpectedRollback` / `Example3_RequiresNew`.
- (트랜잭션 설정·AccountDao는 13.2의 `@Import(TxConfig.class)` 재사용.)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s05_propagation.Example1_Required
./gradlew runStage -Pmain=com.study.part13_tx.s05_propagation.Example2_UnexpectedRollback
./gradlew runStage -Pmain=com.study.part13_tx.s05_propagation.Example3_RequiresNew
```

### 실행 결과 — 가설과 실제 비교 (실측)
```
예제1 (REQUIRED)
  (A) 내부 정상 -> A=900,  B=1100   (함께 커밋)
  (B) 내부 실패 -> A=1000, B=1000   (외부까지 전부 롤백 = 하나의 물리 트랜잭션)

예제2 (함정)
  내부 예외를 catch로 삼킴 -> 커밋 시 UnexpectedRollbackException
  "Transaction rolled back because it has been marked as rollback-only"
  결과 A=1000, B=1000 (전체 롤백)

예제3 (REQUIRES_NEW)
  내부(별도 트랜잭션)만 롤백, 외부 커밋 -> A=900, B=1000 (예외 없음)
```
- REQUIRED는 묶여서 함께 롤백, 삼켜도 UnexpectedRollback, REQUIRES_NEW는 분리되어 외부만 커밋. ✅

---

## 3. 자기 점검

- **Q. REQUIRED의 핵심 동작은?**
  - 내 답: 진행 중 트랜잭션이 있으면 참여(없으면 새로 시작). 외부+내부가 하나의 물리 트랜잭션이라, 내부
    실패가 외부 작업까지 함께 롤백시킨다. 실제 commit/rollback은 외부에서만 일어난다(내부는 논리적).

- **Q. 내부 예외를 try-catch로 잡았는데 왜 UnexpectedRollbackException이 나나?**
  - 내 답: REQUIRED 내부가 실패하면 물리 트랜잭션을 rollbackOnly로 표시한다. 예외를 삼켜도 이 표시는 남아,
    외부 커밋 시점에 발견되어 강제 롤백 + UnexpectedRollbackException이 난다. "논리 하나라도 롤백되면 물리는 롤백".

- **Q. 외부를 살리면서 내부만 롤백하려면?**
  - 내 답: 내부를 REQUIRES_NEW로 분리한다. 별도 물리 트랜잭션이라 내부 롤백이 외부에 영향을 안 준다(외부 커밋).
    단 커넥션을 동시에 2개 점유하므로 풀 고갈에 주의.

- **Q. REQUIRED와 REQUIRES_NEW를 한 줄로 구분하면?**
  - 내 답: REQUIRED=있으면 '참여'(한 트랜잭션), REQUIRES_NEW=항상 '새 트랜잭션'(분리). 실무는 이 둘이 중심.
