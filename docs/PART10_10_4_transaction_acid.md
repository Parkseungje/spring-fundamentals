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

### 트랜잭션과 ACID — 네 글자를 '코드로' 하나씩
ACID는 트랜잭션이 보장해야 할 4가지 성질의 머리글자다. 추상적으로 외우지 말고 "각 글자가 코드에서
무엇으로 나타나는가"로 잡자. 아래 각 성질 끝에 그것을 직접 보여주는 예제를 달아 두었다.

- **A 원자성(Atomicity) = "모두 성공 아니면 모두 실패"** → 실행 '방식'에 대한 약속.
  - 이체(출금+입금)처럼 묶인 작업은 **전부 commit 되거나, 하나라도 실패하면 전부 rollback** 된다. 중간에
    출금만 되고 입금이 안 되는 '반쪽 상태'는 없다.
  - PART 7의 `Atomic`은 '변수 하나'의 원자 연산, DB Atomicity는 '여러 작업(여러 행·테이블) 묶음'이라는 점이 다르다.
  - 👉 코드: `Example1_Atomicity` (도중 실패 → 출금까지 취소).

- **C 일관성(Consistency) = "항상 올바른 상태로만 이동"** → 실행 결과 '상태'에 대한 약속.
  - 트랜잭션이 끝났을 때 DB가 **정의된 모든 규칙(제약조건·불변식)을 여전히 만족**한다. 규칙을 깨는 변경은
    DB가 거부하거나 롤백되어, **잘못된 상태로는 절대 넘어가지 않는다.**
  - 규칙의 종류: ① DB가 스스로 지키는 제약(CHECK·NOT NULL·UNIQUE·FK) ② 비즈니스 불변식(예: "두 계좌 합계 일정").
  - ★ 원자성과의 차이: 원자성은 '어떻게 실행되나(모두/아무것도)', 일관성은 '결과가 규칙에 맞나(올바름)'.
    원자성·격리성·제약조건이 **함께** 작동해 일관성을 결과적으로 만든다(원자성은 일관성의 '도구').
  - 👉 코드: `Example5_Consistency` (음수 잔액 CHECK 위반 거부 / 합계 불변식 유지).

- **I 격리성(Isolation) = "커밋 전 변경은 남에게 안 보임"** → 동시 실행에 대한 약속.
  - 여러 트랜잭션이 동시에 돌아도 **서로 독립적으로 실행되는 것처럼** 보인다. 내가 commit하기 전 변경은
    다른 세션에 안 보인다. 얼마나 강하게 격리할지는 '격리 수준'으로 조절한다(아래 절).
  - 👉 코드: `Example2_CommitVisibility` (커밋 전 옛 값/커밋 후 새 값), `Example3_ReadPhenomena` (격리 수준별 이상현상).

- **D 지속성(Durability) = "커밋된 데이터는 재시작해도 안 사라짐"** → 영속에 대한 약속.
  - 한 번 commit된 변경은 **영구적**이다. DB는 commit 시점에 변경을 디스크(WAL 로그 등)에 안전하게
    기록(fsync)하므로, 그 직후 정전·재시작이 나도 복구된다.
  - ★ 격리성과 헷갈리지 말 것: 둘 다 'commit'을 경계로 하지만 — 격리성은 "commit 전엔 남에게 안 보임",
    지속성은 "commit 후엔 재시작해도 안 사라짐". 시점·관점이 다르다.
  - 👉 코드: `Example6_Durability` (커밋분은 DB 재기동 후 생존, 미커밋은 소멸).

한눈에:

| 글자 | 한 줄 정의 | 관점 | 코드 |
|---|---|---|---|
| A 원자성 | 모두 성공 or 모두 실패 | 실행 방식 | Example1 |
| C 일관성 | 항상 규칙을 만족하는 올바른 상태로만 | 결과 상태 | Example5 |
| I 격리성 | 커밋 전 변경은 남에게 안 보임 | 동시 실행 | Example2, Example3 |
| D 지속성 | 커밋된 데이터는 재시작해도 보존 | 영속 | Example6 |

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

#### 이 이상현상들, 요즘에도 실제로 일어나나? (실전 시나리오)
결론부터: **"옛날에 있던 버그라 지금은 패치돼서 안 난다"가 아니다.** 이상현상은 버그가 아니라 **격리 수준을
낮춰 성능·동시성을 얻는 대가**다. 그래서 **오늘날 운영 중인 시스템에서도 그대로 일어난다.** 특히 대부분의
DB 기본 격리가 READ COMMITTED(H2/Oracle/PostgreSQL)라, 그 환경에선 **Dirty Read만 막히고 Non-Repeatable·
Phantom은 여전히 발생 가능**하다. "안 겪는다"면 트래픽이 적어 동시 충돌이 드물거나, 우연히 타이밍이 안 겹쳤을 뿐이다.

다만 **무엇이 줄었나**는 있다 — 표준이 정의만 했지, 현대 DB의 MVCC 구현이 일부를 추가로 막아 준다. 예: MySQL
InnoDB의 기본 REPEATABLE READ는 갭 락(gap lock)으로 Phantom도 상당 부분 막는다. 그래서 "DB·격리 수준·구현에
따라 발생 범위가 다르다"가 정확한 답이다. 아래는 READ COMMITTED(가장 흔한 기본)에서 실제로 나는 시나리오다.

★ Dirty Read — "있지도 않은 돈을 본 사이트"
> 쇼핑몰 관리자 화면. 정산 트랜잭션이 판매자 잔액을 1,000,000원 올리는 UPDATE를 실행하고 아직 commit 전이다.
> 바로 그 찰나, 다른 배치가 "오늘 총 정산액" 집계를 위해 그 잔액을 읽는다. 만약 이 배치가 READ UNCOMMITTED로
> 돌고 있었다면 **아직 확정도 안 된 1,000,000원을 합계에 포함**한다. 그런데 정산 트랜잭션이 카드사 오류로
> rollback됐다. → 배치가 만든 "오늘 총 정산액"에는 **존재한 적 없는 100만 원**이 섞여 장부가 틀어진다.
> 현실: READ UNCOMMITTED를 일부러 쓰는 일은 드물어 Dirty Read는 실무에서 가장 보기 어렵다. 하지만 "빠르게
> 대충 집계하자"며 격리를 낮추면 바로 이 사고가 난다.

★ Non-Repeatable Read — "결제 직전에 바뀐 잔액"
> 사용자가 '포인트로 결제' 버튼을 누른다. 서버의 결제 트랜잭션은 ① 먼저 포인트 잔액을 읽어 "5,000P 있네,
> 충분해"라고 확인하고 → ② 잠시 다른 검증(재고·쿠폰)을 하느라 수십 ms가 흐른 뒤 → ③ 다시 잔액을 읽어
> 차감하려 한다. 그 사이, **같은 사용자가 다른 탭에서 4,000P를 환불(출금)** 하고 그 트랜잭션이 commit됐다.
> READ COMMITTED라 ③의 재읽기는 커밋된 새 값 1,000P를 본다. → ①에서 "충분"이라 판단하고 진행한 로직이
> ③에서 갑자기 잔액이 달라져 **음수 차감/검증 불일치/예외**가 난다. "분명 아까 읽을 땐 5,000이었는데?"가 이것이다.
> 막는 법: 격리를 REPEATABLE READ로 올리거나, 처음부터 `SELECT ... FOR UPDATE`로 그 행을 락 걸어 두 읽기 사이에 못 바꾸게 한다.

★ Phantom Read — "두 번 센 좌석 수가 다르다"
> 콘서트 예매. 예약 트랜잭션이 ① "이 구역에 남은 좌석 수"를 `count(*) where 구역=A and 상태='빈자리'`로 세니
> 1석. "마지막 1석 잡자" 하고 진행하는데, ② 결제 직전 한 번 더 같은 조건으로 세어 보니 **2석**이 됐다.
> 그 사이 **다른 관리자가 취소표 1장을 빈자리로 insert·commit**했기 때문(없던 행이 유령처럼 나타남).
> 반대로, 두 사용자가 동시에 "남은 1석"을 각자 세고 각자 예약을 진행해 **같은 좌석이 두 번 팔리는** 오버부킹도
> 이 계열의 사고다. 막는 법: SERIALIZABLE, 또는 좌석 테이블에 UNIQUE 제약/범위 락(MySQL 갭 락)으로 동시 삽입을 차단.

정리: 이 셋은 **"동시에 같은 데이터를 건드리는 사용자가 늘수록(=트래픽이 클수록) 실제로 터진다."** 그래서
실무에서는 ① 대부분 READ COMMITTED로 두고 동시성을 살리되, ② "두 번 읽고 그 사이 판단하는" 금전·재고·예약
같은 민감 로직만 골라 격리를 올리거나 `FOR UPDATE`/낙관적 락(@Version, 10.2)으로 콕 집어 보호한다. "전부
SERIALIZABLE"은 안전하지만 동시성이 죽어 거의 안 쓴다.

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
> - **가설 5(C 일관성)**: 음수 잔액(CHECK 위반)은 DB가 거부해 롤백되고, 깨진 불변식(합계≠2000)은 커밋되지 않아 항상 올바른 상태로 남는다.
> - **가설 6(D 지속성)**: 커밋된 데이터는 DB를 닫았다 다시 열어도 살아남고, 커밋 안 한 데이터는 사라진다.

### 코드 (`com.study.part10_db.s04_transaction`) — ACID 각 성질 + 격리/부분롤백
- `Example1_Atomicity` (A 원자성) — 이체를 (A) 도중 실패→rollback / (B) 정상→commit으로 비교.
- `Example5_Consistency` (C 일관성) — (A) CHECK(balance≥0) 위반 거부 / (B) 합계 불변식 유지.
- `Example2_CommitVisibility` (I 격리성) — "커밋 전 변경은 안 보이고, 커밋 후 보인다"(Dirty Read 방지).
- `Example6_Durability` (D 지속성) — 파일 H2로 재기동 후 커밋분 생존/미커밋 소멸.
- `Example3_ReadPhenomena` — 격리 수준별 이상현상 3종 재현((D) READ UNCOMMITTED Dirty Read / (A) READ
  COMMITTED Non-Repeatable / (B) REPEATABLE READ 방지 / (C) Phantom).
- `Example4_Savepoint` — 저장점까지만 부분 롤백(앞 작업 유지).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example1_Atomicity
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example5_Consistency
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example2_CommitVisibility
./gradlew runStage -Pmain=com.study.part10_db.s04_transaction.Example6_Durability
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
(D) READ UNCOMMITTED: 미커밋 9999 읽음 -> writer 롤백 -> 재읽기 1000   = Dirty Read 발생!
(A) READ COMMITTED : 1차=1000 -> (writer 2000 커밋) -> 2차=2000   = Non-Repeatable Read 발생!
(B) REPEATABLE READ: 1차=1000 -> (writer 2000 커밋) -> 2차=1000   = 반복 가능(막아줌)
(C) READ COMMITTED : 행수 2 -> (writer C 삽입 커밋) -> 행수 3      = Phantom Read 발생!
```
Example4 (Savepoint) 실측:
```
A 100 출금 -> savepoint -> B 100 입금 -> rollback(sp) -> commit
결과: A=900, B=1000   (A 출금만 반영, B 입금은 취소 = 부분 롤백)
```
Example5 (C 일관성) 실측:
```
(A) A에서 1500 출금 시도 -> CHECK(balance>=0) 위반 거부 -> 롤백 -> A=1000 (음수 안 됨)
(B) A 300 출금 후 입금 실패 -> 롤백 -> 합계 2000 유지 (불변식 보존)
```
Example6 (D 지속성) 실측:
```
1차: insert(1 커밋) + insert(2 미커밋) 후 DB 종료
2차(재기동): id=1만 존재  -> 커밋분 생존(true)=지속성, 미커밋 소멸(false)
```
- 가설 1: 실패 시 출금까지 취소(원자성), 성공 시 둘 다 반영. ✅
- 가설 2: commit 전엔 옛 값, commit 후 새 값(격리성, H2 기본 READ COMMITTED). ✅
- 가설 3: READ COMMITTED에서 Non-Repeatable/Phantom 발생, REPEATABLE READ가 Non-Repeatable 방지. ✅
- 가설 4: 저장점 이후(B 입금)만 취소, 앞 작업(A 출금)은 유지. ✅
- 가설 5: CHECK 위반 거부로 음수 잔액 안 됨, 깨진 합계는 커밋 안 됨(일관성). ✅
- 가설 6: 커밋분(1번) 재기동 후 생존, 미커밋(2번) 소멸(지속성). ✅

---

## 3. 자기 점검

- **Q. ACID 4가지를 한 줄씩 + 각각 코드로 어떻게 확인했나?**
  - 내 답: A 원자성=모두 성공 or 모두 실패(Example1, 도중 실패 시 출금까지 취소). C 일관성=항상 규칙(제약·
    불변식)을 만족하는 올바른 상태로만 이동(Example5, CHECK 위반 거부·합계 유지). I 격리성=커밋 전 변경은
    남에게 안 보임(Example2). D 지속성=커밋된 데이터는 재시작해도 보존(Example6, 파일 H2 재기동 후 생존).

- **Q. 원자성과 일관성은 뭐가 다른가?**
  - 내 답: 원자성은 '실행 방식'(모두/아무것도), 일관성은 '결과 상태가 규칙에 맞는가(올바름)'. 원자성·격리성·
    제약조건이 함께 작동해 일관성을 만든다(원자성은 일관성을 지키는 도구). 깨진 중간 상태를 커밋 안 함으로써 일관성 유지.

- **Q. 격리성과 지속성은 둘 다 commit이 기준인데 뭐가 다른가?**
  - 내 답: 격리성은 "commit '전'엔 다른 세션에 안 보임", 지속성은 "commit '후'엔 재시작해도 안 사라짐".
    관점과 시점이 다르다. (Example2 vs Example6)

- **Q. DB의 Atomicity와 PART 7의 Atomic의 차이는?**
  - 내 답: PART 7 Atomic은 '변수 하나'의 원자 연산, DB Atomicity는 '여러 작업(여러 행/테이블) 묶음'을
    모두-or-아무것도로 보장. 범위가 다르다.

- **Q. Dirty Read가 무엇이고 어느 격리 수준에서 막히나?**
  - 내 답: 다른 트랜잭션이 아직 커밋 안 한 값을 읽는 것. READ UNCOMMITTED에서 발생, READ COMMITTED부터 막힌다.

- **Q. 읽기 이상현상 3종과 각각을 막는 격리 수준은?**
  - 내 답: Dirty Read(미커밋 값 읽기, READ COMMITTED부터 방지) / Non-Repeatable Read(같은 행 재읽기 시 값
    바뀜, REPEATABLE READ부터 방지) / Phantom Read(같은 조건 재조회 시 행 수 바뀜, SERIALIZABLE에서 방지).
    위로 갈수록 빠르지만 위험, 아래로 갈수록 안전하지만 느리다.

- **Q. 이 이상현상들은 요즘에도 실제로 일어나나?**
  - 내 답: 그렇다. 버그가 아니라 격리 수준을 낮춰 성능을 얻는 대가라, 지금도 발생한다. 대부분 DB 기본이
    READ COMMITTED라 Dirty Read만 막히고 Non-Repeatable·Phantom은 동시 트래픽이 많을수록 실제로 터진다.
    (MVCC·갭 락이 일부 더 막아 주지만 DB·구현마다 다름.) 실무는 민감 로직만 골라 격리를 올리거나
    FOR UPDATE/낙관적 락(@Version)으로 보호한다. 예: 결제 직전 잔액이 바뀌는 Non-Repeatable, 좌석 오버부킹 Phantom.

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
