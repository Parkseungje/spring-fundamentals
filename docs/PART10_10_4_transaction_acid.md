# PART 10 — DB 접근의 진화: 10.4 트랜잭션과 ACID

> 이 문서는 커리큘럼 PART 10의 소단원 중 **10.4 트랜잭션과 ACID**를 다룬다.
> (커리큘럼 "★ 면접 단골" 구간) 여러 작업을 '한 묶음'으로 안전하게 처리하는 트랜잭션과 그 성질(ACID),
> 격리 수준을 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **트랜잭션(transaction)**: 한 단위로 취급되는 작업 묶음. 예: 계좌 이체 = 출금 + 입금(둘 다 되거나 둘 다 안 되거나).
- **Commit**: 트랜잭션의 변경을 영구 반영. **Rollback**: 변경을 전부 취소(없던 일로).
- **AutoCommit**: 기본은 쿼리 한 줄마다 자동 커밋. 묶으려면 `setAutoCommit(false)`로 끄고 내가 commit/rollback.
- **ACID**: 트랜잭션이 보장해야 할 4성질 — **A**tomicity(원자성)·**C**onsistency(일관성)·**I**solation(격리성)·**D**urability(지속성).
- **격리 수준(isolation level)**: 동시 트랜잭션끼리 서로를 얼마나 차단하나. READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE.
- **Dirty Read**: 다른 트랜잭션이 '아직 커밋 안 한' 값을 읽어버리는 것(낮은 격리 수준에서 발생).

한 줄 그림: **트랜잭션은 여러 작업을 '모두 성공 or 모두 실패'로 묶는다(원자성). 커밋 전 변경은 다른 세션에
안 보이고(격리성), 커밋된 데이터는 영구 보존된다(지속성). 격리 수준으로 동시성 안전도를 조절한다.**

---

## 1. 학습 내용 — 묶어서 안전하게

### 트랜잭션과 ACID
- **A 원자성(Atomicity)**: 묶음이 **모두 성공(commit) 또는 모두 실패(rollback)**. 이체 도중 실패하면
  출금도 없던 일로. (PART 7의 Atomic은 '변수 하나', DB Atomicity는 '여러 작업 묶음'이라는 점이 다름)
- **C 일관성(Consistency)**: 트랜잭션 완료 후에도 제약조건(NOT NULL/UNIQUE/FK/비즈니스 규칙)이 항상 지켜짐.
- **I 격리성(Isolation)**: 각 트랜잭션은 독립적으로 실행되는 것처럼 보임. 커밋 전 변경은 남에게 안 보임.
- **D 지속성(Durability)**: commit된 데이터는 영구 보존(WAL 로그·fsync). 시스템이 다운돼도 살아남음.

### JDBC로 트랜잭션 다루기
```java
conn.setAutoCommit(false);   // 트랜잭션 시작(자동 커밋 끔)
try {
    update(...);             // 작업 1
    update(...);             // 작업 2
    conn.commit();          // 둘 다 성공 -> 영구 반영
} catch (Exception e) {
    conn.rollback();        // 중간 실패 -> 전체 취소(작업 1도 없던 일로)
}
```

### 격리 수준과 Dirty Read
동시 트랜잭션이 서로를 얼마나 차단하느냐를 정한다. 높을수록 안전하지만 느리다(동시성 ↓).

| 격리 수준 | 특징 |
|---|---|
| READ UNCOMMITTED | 커밋 안 된 값도 읽음 → **Dirty Read** 발생(위험) |
| READ COMMITTED | 커밋된 값만 읽음(H2/Oracle 기본). Dirty Read 방지 |
| REPEATABLE READ | 한 트랜잭션 안에서 같은 행을 다시 읽어도 동일(MySQL 기본) |
| SERIALIZABLE | 완전 직렬화(가장 안전, 가장 느림) |

- **커밋 이전 격리**: 세션1이 값을 바꿔도 commit 전이면 세션2는 **변경 전 값**을 본다(READ COMMITTED).
  비유: 각자 노트북에서 작업(미커밋) → 클라우드에 저장(commit)해야 남이 본다.

---

## 2. 실습으로 확인하기

> - **가설 1**: 이체 중 실패하면 rollback으로 출금·입금이 둘 다 취소된다(원자성). 정상이면 둘 다 반영(commit).
> - **가설 2**: 세션1이 commit하기 전엔 세션2가 옛 값을 보고, commit 후에야 새 값을 본다(격리성).

### 코드 (`com.study.part10_db.s04_transaction`)
- `Example1_Atomicity` — 이체를 (A) 도중 실패→rollback / (B) 정상→commit으로 비교.
- `Example2_CommitVisibility` — 두 세션으로 "커밋 전 변경은 안 보이고, 커밋 후 보인다" 확인.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example1_Atomicity
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example2_CommitVisibility
```

### 실행 결과 — 가설과 실제 비교
```
(A) 이체 300 (도중 예외 -> rollback): 결과 A=1000, B=1000  (둘 다 원래대로 = 원자성)
(B) 이체 300 (정상 -> commit):       결과 A=700,  B=1300  (둘 다 반영)
```
```
세션2가 읽은 A 잔액 (commit 전) = 1000   <- 옛 값! 미커밋 변경은 안 보임
세션2가 읽은 A 잔액 (commit 후) = 9999   <- 이제 새 값 보임
```
- 가설 1: 실패 시 출금까지 취소(원자성), 성공 시 둘 다 반영. ✅
- 가설 2: commit 전엔 옛 값, commit 후 새 값(격리성, H2 기본 READ COMMITTED). ✅

---

## 3. 자기 점검

- **Q. ACID 4가지를 한 줄씩 설명하면?**
  - 내 답: A=모두 성공 or 모두 실패, C=완료 후 제약/규칙 항상 유지, I=트랜잭션끼리 독립(커밋 전 변경 안 보임),
    D=커밋된 데이터는 영구 보존.

- **Q. DB의 Atomicity와 PART 7의 Atomic의 차이는?**
  - 내 답: PART 7 Atomic은 '변수 하나'의 원자 연산, DB Atomicity는 '여러 작업(여러 행/테이블) 묶음'을
    모두-or-아무것도로 보장. 범위가 다르다.

- **Q. Dirty Read가 무엇이고 어느 격리 수준에서 막히나?**
  - 내 답: 다른 트랜잭션이 아직 커밋 안 한 값을 읽는 것. READ UNCOMMITTED에서 발생, READ COMMITTED부터 막힌다.

- **Q. setAutoCommit(false)를 안 하면 왜 rollback이 의미 없나?**
  - 내 답: 자동 커밋이 켜져 있으면 쿼리 한 줄마다 즉시 커밋되어, 묶음으로 취소할 게 없다. 트랜잭션으로
    묶으려면 자동 커밋을 꺼야 commit/rollback이 묶음 단위로 작동한다.
