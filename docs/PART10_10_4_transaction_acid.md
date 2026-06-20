# PART 10 — DB 접근의 진화: 10.4 트랜잭션과 ACID

> 이 문서는 커리큘럼 PART 10의 소단원 중 **10.4 트랜잭션과 ACID**를 다룬다.
> (커리큘럼 "★ 면접 단골" 구간) 여러 작업을 '한 묶음'으로 안전하게 처리하는 트랜잭션과 그 성질(ACID),
> 격리 수준과 3대 읽기 이상현상(Dirty/Non-Repeatable/Phantom), MVCC, Savepoint(부분 롤백)를 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **트랜잭션(transaction)**: 한 단위로 취급되는 작업 묶음. 예: 계좌 이체 = 출금 + 입금(둘 다 되거나 둘 다 안 되거나).
- **Commit**: 트랜잭션의 변경을 영구 반영. **Rollback**: 변경을 전부 취소(없던 일로).
- **AutoCommit**: 기본은 쿼리 한 줄마다 자동 커밋. 묶으려면 `setAutoCommit(false)`로 끄고 내가 commit/rollback.
- **ACID**: 트랜잭션이 보장해야 할 4성질 — **A**tomicity(원자성)·**C**onsistency(일관성)·**I**solation(격리성)·**D**urability(지속성).
- **격리 수준(isolation level)**: 동시 트랜잭션끼리 서로를 얼마나 차단하나. READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE.
- **읽기 이상현상(read phenomena)**: 동시 트랜잭션 때문에 생기는 잘못된 읽기 3종 — Dirty Read / Non-Repeatable Read / Phantom Read.
- **Dirty Read**: 다른 트랜잭션이 '아직 커밋 안 한' 값을 읽어버리는 것(낮은 격리 수준에서 발생).
- **Non-Repeatable Read(반복 불가능 읽기)**: 같은 '행'을 두 번 읽는 사이 다른 트랜잭션이 수정·커밋해 값이 달라짐.
- **Phantom Read(팬텀 리드)**: 같은 '조건'으로 두 번 조회하는 사이 다른 트랜잭션이 행을 삽입·삭제해 결과 행 수가 달라짐.
- **MVCC(다중 버전 동시성 제어)**: 락 대신 데이터의 '스냅샷(버전)'을 읽게 해 격리성을 구현하는 방식.
- **Savepoint(저장점)**: 트랜잭션 안의 한 지점. 거기까지만 '부분 롤백'할 수 있다.

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

### 격리 수준과 3대 읽기 이상현상
격리 수준은 "동시에 도는 트랜잭션이 서로를 얼마나 차단하느냐"를 정한다. 차단을 약하게 하면 빠르지만(동시성 ↑)
'잘못된 읽기(이상현상)'가 생기고, 강하게 하면 안전하지만 느리다. 그래서 **각 격리 수준은 "어떤 이상현상까지
막아주느냐"로 정의된다.** 먼저 막아야 할 이상현상 3종부터 보자(같은 데이터를 두 번 읽는 사이 다른 트랜잭션이
끼어드는 상황).

- **Dirty Read(더티 리드)**: 다른 트랜잭션이 **아직 커밋도 안 한** 값을 읽어버린다. 그 트랜잭션이 롤백하면
  나는 '존재한 적 없는 값'을 읽은 꼴 → 가장 위험. (예: 송금 중 잠깐 보인 금액을 읽었는데 그 송금이 취소됨)
- **Non-Repeatable Read(반복 불가능 읽기)**: 같은 **행**을 두 번 읽었는데, 그 사이 다른 트랜잭션이 그 행을
  **수정·커밋**해서 두 번째 값이 달라진다. "한 트랜잭션 안에서는 같은 걸 읽으면 같아야 하는데" 그게 깨짐.
- **Phantom Read(팬텀 리드)**: 같은 **조건**으로 두 번 조회했는데, 그 사이 다른 트랜잭션이 행을 **삽입·삭제·
  커밋**해서 결과 '행 개수'가 달라진다. 없던 행이 유령(phantom)처럼 나타나거나 사라진다.

> ★ Non-Repeatable vs Phantom 헷갈림 정리 — 둘 다 "두 번 읽으니 달라졌다"지만 대상이 다르다.
> Non-Repeatable은 **이미 읽은 그 행의 '값'이 바뀐 것**(update), Phantom은 **조회 결과의 '행 집합(개수)'이
> 바뀐 것**(insert/delete). "값이 바뀌면 Non-Repeatable, 행이 생기거나 사라지면 Phantom".

#### 격리 수준 × 이상현상 매트릭스 (★ 면접 단골)
각 격리 수준이 어떤 현상을 막는지(O = 막음/발생 안 함, X = 막지 못함/발생 가능):

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read | 비고 |
|---|---|---|---|---|
| READ UNCOMMITTED | X(발생) | X | X | 가장 빠르고 위험. 거의 안 씀 |
| READ COMMITTED | O(막음) | X | X | 커밋된 값만 읽음. H2/Oracle/PostgreSQL 기본 |
| REPEATABLE READ | O | O | X(이론상)* | 같은 행 재읽기 보장. MySQL(InnoDB) 기본 |
| SERIALIZABLE | O | O | O | 완전 직렬화. 가장 안전·가장 느림 |

- 위로 갈수록 빠르지만 이상현상에 취약, 아래로 갈수록 안전하지만 동시성·성능이 떨어진다.
- *MySQL InnoDB의 REPEATABLE READ는 MVCC + 갭 락으로 실무상 Phantom도 상당 부분 막는다(표준 정의와 약간 다름).
  표는 SQL 표준 기준이며, "DB마다 구현이 조금씩 다르다"는 점을 기억하면 된다.

#### MVCC — 락 없이 격리성을 구현하는 방식
높은 격리(REPEATABLE READ)를 "모든 읽기에 락을 거는" 방식으로 구현하면 너무 느리다. 그래서 현대 DB
(PostgreSQL/MySQL InnoDB 등)는 **MVCC(다중 버전 동시성 제어)** 를 쓴다.
- 데이터를 수정할 때 기존 값을 덮어쓰지 않고 **새 버전을 만들고**, 각 트랜잭션은 "자기가 시작한 시점의
  스냅샷(버전)"을 읽는다. → 읽기가 쓰기를 막지 않고, 쓰기가 읽기를 막지 않아 동시성이 높다.
- REPEATABLE READ에서 같은 행을 다시 읽어도 값이 그대로인 이유가 바로 이 스냅샷이다(다른 트랜잭션이 새
  버전을 커밋해도 내 스냅샷은 그대로). 실측 Example3의 (B)에서 reader가 두 번 다 1000을 본 게 이 동작이다.
- (10.2 1-7에서 "일반 SELECT가 락을 거의 안 건다"고 한 것이 MVCC 덕분이다.)

- **커밋 이전 격리**: 세션1이 값을 바꿔도 commit 전이면 세션2는 **변경 전 값**을 본다(READ COMMITTED).
  비유: 각자 노트북에서 작업(미커밋) → 클라우드에 저장(commit)해야 남이 본다.

### Savepoint — 부분 롤백
보통 `rollback()`은 트랜잭션 '전체'를 취소한다. 하지만 "앞부분 작업은 살리고 뒷부분만 취소"하고 싶을 때가
있다. 그때 **Savepoint(저장점)** 를 찍어 두고, 문제가 생기면 그 지점까지만 되돌린다.
```java
conn.setAutoCommit(false);
update("A에서 100 출금");              // 살리고 싶은 작업
Savepoint sp = conn.setSavepoint();   // 저장점
update("B에 100 입금");                // 취소될 수 있는 작업
conn.rollback(sp);                    // 저장점 이후만 취소 (A 출금은 유지)
conn.commit();                        // -> A 출금만 반영
```
- 전체 롤백과 달리 **저장점 이후 작업만 선택 취소**한다. 한 트랜잭션 안에서 일부 단계만 재시도/취소할 때 유용.
- (스프링 `@Transactional`의 중첩 트랜잭션 `PROPAGATION_NESTED`가 내부적으로 이 Savepoint를 쓴다 — PART 13.)

---

## 2. 실습으로 확인하기

> - **가설 1**: 이체 중 실패하면 rollback으로 출금·입금이 둘 다 취소된다(원자성). 정상이면 둘 다 반영(commit).
> - **가설 2**: 세션1이 commit하기 전엔 세션2가 옛 값을 보고, commit 후에야 새 값을 본다(격리성).
> - **가설 3**: READ COMMITTED에선 Non-Repeatable Read·Phantom Read가 발생하고, 격리 수준을 REPEATABLE
>   READ로 올리면 Non-Repeatable Read가 막힌다.
> - **가설 4**: Savepoint로 부분 롤백하면 저장점 이후 작업만 취소되고 앞 작업은 유지된다.

### 코드 (`com.study.part10_db.s04_transaction`)
- `Example1_Atomicity` — 이체를 (A) 도중 실패→rollback / (B) 정상→commit으로 비교.
- `Example2_CommitVisibility` — 두 세션으로 "커밋 전 변경은 안 보이고, 커밋 후 보인다" 확인(Dirty Read 방지).
- `Example3_ReadPhenomena` — (A) READ COMMITTED에서 Non-Repeatable Read 발생 / (B) REPEATABLE READ에서
  방지 / (C) Phantom Read 발생.
- `Example4_Savepoint` — 저장점까지만 부분 롤백(앞 작업 유지).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example1_Atomicity
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example2_CommitVisibility
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example3_ReadPhenomena
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example4_Savepoint
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
Example3 (이상현상) 실측:
```
(A) READ COMMITTED : 1차=1000 -> (writer 2000 커밋) -> 2차=2000   = Non-Repeatable Read 발생!
(B) REPEATABLE READ: 1차=1000 -> (writer 2000 커밋) -> 2차=1000   = 반복 가능(막아줌)
(C) READ COMMITTED : 행수 2 -> (writer C 삽입 커밋) -> 행수 3      = Phantom Read 발생!
```
Example4 (Savepoint) 실측:
```
A 100 출금 -> savepoint -> B 100 입금 -> rollback(sp) -> commit
결과: A=900, B=1000   (A 출금만 반영, B 입금은 취소 = 부분 롤백)
```
- 가설 1: 실패 시 출금까지 취소(원자성), 성공 시 둘 다 반영. ✅
- 가설 2: commit 전엔 옛 값, commit 후 새 값(격리성, H2 기본 READ COMMITTED). ✅
- 가설 3: READ COMMITTED에서 Non-Repeatable/Phantom 발생, REPEATABLE READ가 Non-Repeatable 방지. ✅
- 가설 4: 저장점 이후(B 입금)만 취소, 앞 작업(A 출금)은 유지. ✅

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

- **Q. 읽기 이상현상 3종과 각각을 막는 격리 수준은?**
  - 내 답: Dirty Read(미커밋 값 읽기, READ COMMITTED부터 방지) / Non-Repeatable Read(같은 행 재읽기 시 값
    바뀜, REPEATABLE READ부터 방지) / Phantom Read(같은 조건 재조회 시 행 수 바뀜, SERIALIZABLE에서 방지).
    위로 갈수록 빠르지만 위험, 아래로 갈수록 안전하지만 느리다.

- **Q. Non-Repeatable Read와 Phantom Read의 차이는?**
  - 내 답: 둘 다 두 번 읽으니 달라진 것이지만, Non-Repeatable은 '이미 읽은 행의 값'이 update로 바뀐 것,
    Phantom은 '조회 결과의 행 집합(개수)'이 insert/delete로 바뀐 것. 값이 바뀌면 Non-Repeatable, 행이
    생기거나 사라지면 Phantom. (Example3의 A/B vs C)

- **Q. MVCC는 무엇이고 왜 쓰나?**
  - 내 답: 데이터를 덮어쓰지 않고 새 버전을 만들어, 각 트랜잭션이 시작 시점의 스냅샷을 읽게 하는 방식.
    읽기·쓰기가 서로를 막지 않아 락 방식보다 동시성이 높다. REPEATABLE READ에서 같은 행 재읽기가 보장되는
    이유. (Example3 B에서 reader가 두 번 다 1000을 본 것)

- **Q. Savepoint는 일반 rollback과 무엇이 다른가?**
  - 내 답: 일반 rollback은 트랜잭션 전체 취소, Savepoint는 지정한 저장점 이후 작업만 부분 취소(앞 작업 유지).
    한 트랜잭션에서 일부 단계만 되돌리고 싶을 때 쓴다. 스프링 PROPAGATION_NESTED가 내부적으로 이를 사용. (Example4)

- **Q. setAutoCommit(false)를 안 하면 왜 rollback이 의미 없나?**
  - 내 답: 자동 커밋이 켜져 있으면 쿼리 한 줄마다 즉시 커밋되어, 묶음으로 취소할 게 없다. 트랜잭션으로
    묶으려면 자동 커밋을 꺼야 commit/rollback이 묶음 단위로 작동한다.
