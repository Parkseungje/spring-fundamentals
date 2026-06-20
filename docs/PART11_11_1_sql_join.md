# PART 11 — ORM/JPA와 트랜잭션 추상화: 11.1 SQL JOIN

> 이 문서는 커리큘럼 PART 11의 소단원 중 **11.1 SQL JOIN**을 다룬다.
> ORM/JPA로 가기 전에, 관계형 DB의 본질인 "여러 테이블을 합치는 JOIN"을 먼저 잡는다. (JPA의 연관관계가
> 결국 이 JOIN을 자동 생성하는 것이기 때문)

---

## 0. 들어가기 전에 — 핵심 용어
- **정규화(normalization)**: 중복을 없애려고 데이터를 여러 테이블로 나누는 설계. 그래서 정보가 흩어진다.
- **외래 키(FK, Foreign Key)**: 한 테이블이 다른 테이블의 행을 가리키는 컬럼(예: employee.dept_id → department.id).
- **JOIN**: 흩어진 여러 테이블을 FK 등으로 연결해 한 결과로 합치는 것.
- **INNER / LEFT / RIGHT / FULL OUTER JOIN**: 어느 쪽 행을 남기느냐의 차이(아래 표).
- **ON vs WHERE**: ON은 '두 테이블을 잇는 매칭 규칙', WHERE는 '합쳐진 결과를 거르는 필터'. OUTER JOIN에선 의미가 다르다.

- **카테시안 곱(CROSS JOIN)**: 두 테이블의 모든 행 조합. JOIN의 본질(= 카테시안 곱 + ON 필터).
- **행 뻥튀기**: 1:N 조인 시 '1'쪽 행이 'N' 개수만큼 중복되는 현상(DISTINCT로 제거).
- **중간(조인) 테이블**: 다대다(N:M)를 1:N+N:1로 풀기 위해 (A,B) 쌍을 저장하는 테이블.
- **UNION / UNION ALL**: 두 조회 결과를 '세로로'(행을 이어) 합치는 집합 연산. UNION=중복 제거, UNION ALL=중복 유지. (JOIN=가로 결합과 대비)
- **GROUP BY / 집계 함수**: 같은 값끼리 행을 묶어 그룹마다 집계(COUNT/SUM/AVG/MAX/MIN)하는 것.
- **HAVING**: 그룹으로 묶은 뒤 그 집계 결과(그룹)를 거르는 필터. (WHERE는 묶기 전 개별 행을 거름)

한 줄 그림: **정규화로 정보가 여러 테이블에 나뉘면, JOIN으로 다시 합친다. INNER=양쪽 매칭만(교집합),
LEFT=왼쪽 전부(없으면 NULL). 'LEFT + WHERE NULL'로 매칭 안 되는 행을 찾는다. JOIN의 본질은 '카테시안
곱 + ON 필터'이고, 1:N은 행이 뻥튀기(→DISTINCT), N:M은 중간 테이블로 푼다(→JPA 연관관계의 토대).**

---

## 1. 학습 내용 — 흩어진 정보를 합치기

### 왜 JOIN인가
관계형 DB는 **정규화**로 중복을 제거한다. 예를 들어 직원마다 부서명을 반복 저장하지 않고, 부서는
`department` 테이블에 따로 두고 직원은 `dept_id`(외래 키)로 가리킨다. 그러면 "직원과 부서명을 함께" 보려면
두 테이블을 **JOIN**으로 연결해야 한다. (객체로 치면 `employee.department` 참조에 해당하는 것이 DB의 FK + JOIN이다.)

### JOIN 종류
| JOIN | 의미 |
|---|---|
| INNER | 양쪽에 다 매칭되는 행만(교집합) |
| LEFT | 왼쪽 테이블 전부 + 매칭되는 오른쪽(없으면 NULL) |
| RIGHT | 오른쪽 전부 + 매칭(거의 안 씀, LEFT로 뒤집어 표현) |
| FULL OUTER | 양쪽 전부(합집합) — MySQL 미지원(LEFT UNION RIGHT로 대체) |

- 실무에선 **LEFT JOIN이 가장 빈번**하다(왼쪽 기준 데이터를 다 보면서 연관을 붙임). INNER는 집계·교집합에, RIGHT는 거의 안 쓴다.
- **활용 — "매칭 안 되는 행 찾기"**: "부서 없는 직원" = LEFT JOIN 후 부서 쪽이 NULL인 행만(`WHERE d.id IS NULL`).
- **ON ≠ WHERE**: OUTER JOIN에서 ON은 '매칭 규칙'(안 맞아도 왼쪽은 남음), WHERE는 '결과 필터'(조건 안 맞으면 제거).
  그래서 부서 없는 직원을 ON에 넣는지 WHERE에 넣는지에 따라 결과가 달라진다.

### JOIN의 본질 — 카테시안 곱(CROSS JOIN) + ON 필터
JOIN이 "마법처럼 합쳐 주는 것" 같지만, 내부는 단순하다. **두 테이블의 '모든 행 조합'을 만든 뒤(카테시안
곱), ON 조건으로 걸러내는 것**이다.
- **CROSS JOIN(카테시안 곱)**: 거르기 '전' 상태 = 모든 조합. 직원 3 × 부서 2 = **6행**.
- **INNER JOIN = CROSS JOIN + ON 필터**: 그 6개 조합 중 `e.dept_id = d.id`인 것만 남긴 것.
- ★ 함정: 그래서 **ON을 빠뜨리면(또는 매칭 조건을 잘못 쓰면) 결과가 N×M으로 폭발**한다. 큰 테이블끼리
  실수로 카테시안 곱이 나면 수백만~수십억 행이 되어 DB가 마비된다. "JOIN했는데 행이 이상하게 많다"의 1순위 원인.

### 1:N JOIN의 '행 뻥튀기'와 DISTINCT (★ JPA 복선)
부서(1)에 직원(N)이 매달린 **1:N 관계**를 '부서 기준'으로 조인하면, **부서 행이 직원 수만큼 중복**된다.
```
개발 부서에 직원 2명(김개발, 최개발) -> 부서를 직원과 조인하면:
  개발 | 김개발
  개발 | 최개발     <- '개발'이 2번! (직원 수만큼 뻥튀기)
  영업 | 이영업
```
- 직원 정보까지 함께 보려면 자연스럽지만, **부서 목록만** 원하면 중복이 생긴다 → `SELECT DISTINCT`로 제거.
- ★ 이것이 **PART 14 JPA의 '컬렉션 페치 조인 중복' 문제의 뿌리**다. JPA에서 부서 엔티티 하나에 직원
  컬렉션을 페치 조인하면, 이 SQL 뻥튀기 때문에 **부서 엔티티가 직원 수만큼 중복**되어 나온다 → JPQL의
  `distinct`로 푼다. JOIN의 이 성질을 모르면 14에서 "왜 같은 엔티티가 여러 번?"을 다시 묻게 된다.

### N:M(다대다)과 중간(조인) 테이블 (★ JPA 복선)
학생과 과목은 **다대다**다(한 학생이 여러 과목 수강, 한 과목에 여러 학생). 관계형 DB는 다대다를 **직접
표현하지 못한다**(어느 쪽에 FK를 둘 수 없음). 그래서 **중간 테이블(조인 테이블)** 에 `(학생, 과목)` 쌍을 저장해
**1:N + N:1 두 관계로 쪼개** 푼다.
```
student        enrollment(중간)      course
  학생A   <---  (학생A, 자바)   --->  자바
  학생A   <---  (학생A, DB)     --->  DB
  학생B   <---  (학생B, 자바)   --->  자바
```
- 조회는 **세 테이블을 이어서** JOIN한다: `student → enrollment → course`.
- ★ 이것이 **JPA `@ManyToMany`(또는 중간 엔티티)의 토대**다. 실무에선 중간 테이블에 부가 컬럼(수강일·성적
  등)이 붙는 경우가 많아, @ManyToMany보다 **중간 테이블을 엔티티로 직접 모델링**하는 걸 권장한다(PART 14에서 다룸).

### UNION — 행을 '세로로' 합치기 (JOIN과의 차이)
JOIN과 자주 헷갈리지만 **방향이 다르다.**
- **JOIN**: 두 테이블을 **가로로** 합친다 — 한 행에 양쪽 컬럼을 옆으로 이어 붙인다(열이 늘어남).
- **UNION**: 두 **조회 결과**를 **세로로** 합친다 — 한 결과 아래에 다른 결과의 행을 이어 붙인다(행이 늘어남).
```
JOIN (가로):  [emp][dept]  <- 한 행에 두 테이블 컬럼이 옆으로
UNION (세로): 조회A의 행들
              조회B의 행들  <- 같은 컬럼 구조로 아래에 이어 붙임
```
- **전제 조건**: 두 SELECT의 **컬럼 수와 타입이 같아야** 한다(세로로 쌓으려면 모양이 맞아야 하니까).
- **UNION vs UNION ALL**:
  - `UNION` — 합친 뒤 **중복 행을 제거**한다(내부적으로 정렬/중복 검사 → 약간 느림).
  - `UNION ALL` — **중복도 그대로** 둔다(검사 안 함 → 더 빠름). "중복이 없거나 상관없으면 UNION ALL이 낫다".
- 실측: 개발팀 직원(김개발·최개발)과 이름에 '개발' 포함(김개발·최개발)을 합치면 → UNION은 **2건**(중복 제거),
  UNION ALL은 **4건**(중복 유지).

#### UNION의 대표 활용 — FULL OUTER JOIN 흉내
"양쪽의 매칭 안 되는 행을 **모두** 보고 싶다"가 FULL OUTER JOIN인데, MySQL은 이를 **지원하지 않는다**. 그래서
**'A 기준 LEFT' UNION 'B 기준 LEFT'** 로 흉내 낸다(UNION이 겹치는 매칭 행은 합쳐 준다).
```sql
select e.name emp, d.name dept from employee e left join department d on e.dept_id = d.id  -- 직원 전부
union
select e.name emp, d.name dept from department d left join employee e on e.dept_id = d.id  -- 부서 전부
```
- 직원 없는 부서 '인사'를 추가하고 실행하면, **두 종류의 NULL이 모두** 나온다:
  - 박무소속 | NULL ← 부서 없는 직원(왼쪽 first 쿼리에서)
  - NULL | 인사 ← 직원 없는 부서(두 번째 쿼리에서)
- 이렇게 LEFT 두 방향을 UNION하면 FULL OUTER와 같은 효과("양쪽 다, 안 맞는 것도 포함")가 난다.

> ★ 정리 — JOIN으로 풀 수 없는 게 UNION이다. "여러 테이블의 정보를 한 행에 붙이고 싶다"=JOIN(가로),
> "여러 조회 결과를 한 목록으로 쌓고 싶다"=UNION(세로). 둘은 보완 관계지 대체 관계가 아니다.

### GROUP BY — 그룹별로 묶어 집계하기 (JOIN과 자주 결합)
지금까지는 행을 '합치고 거르는' 것이었다. GROUP BY는 한 발 더 나가 **같은 값끼리 행을 묶어, 그룹마다 하나의
집계값을 낸다.** "부서별 직원 수", "부서별 평균 연봉" 같은 통계가 전형적이다.
- **집계 함수**: `COUNT`(개수), `SUM`(합), `AVG`(평균), `MAX`/`MIN`(최대/최소).
- **GROUP BY 컬럼**: 그 컬럼의 같은 값을 한 그룹으로 묶는다. 결과는 '그룹당 한 행'이 된다.
```sql
select dept_id, count(*) cnt from employee group by dept_id;
-- dept_id가 같은 직원끼리 묶어 그룹마다 인원수를 센다
```
- 실측(employee 기준): 개발(dept 1)=2, 영업(dept 2)=1, 무소속(NULL)=1. → 5명이 3그룹으로 묶여 3행이 된다.

#### JOIN + GROUP BY — "부서 '이름'별 직원 수"
dept_id(숫자) 말고 부서 '이름'으로 집계하려면 department와 **JOIN한 뒤 묶는다.**
```sql
select d.name dept, count(e.id) cnt
from department d
left join employee e on e.dept_id = d.id
group by d.name;
```
- LEFT JOIN을 쓰면 **직원이 0명인 부서(인사)도 결과에 포함**된다(INNER면 빠진다).
- ★ 함정 — `count(*)` vs `count(컬럼)`: LEFT JOIN에서 직원 없는 부서는 'e 쪽이 전부 NULL인 한 행'으로 남는다.
  `count(*)`는 그 NULL 행도 1로 세어 **인사가 1로** 잘못 나온다. 직원 수를 정확히 세려면 **`count(e.id)`** 처럼
  '직원 컬럼'을 세야 한다(NULL은 안 셈) → 인사=0. (실측: 개발 2, 영업 1, 인사 0)

#### WHERE vs HAVING — 거르는 시점이 다르다
- **WHERE**: 그룹으로 **묶기 전**, 개별 행을 거른다.
- **HAVING**: 그룹으로 **묶은 뒤**, 집계 결과(그룹)를 거른다. 집계 함수 조건은 HAVING에만 쓸 수 있다.
```sql
select d.name dept, count(e.id) cnt from department d
left join employee e on e.dept_id = d.id
group by d.name having count(e.id) >= 2;   -- 직원 2명 이상인 부서만
```
- 실측: 개발(2)만 남는다. (WHERE에는 `count(...) >= 2`를 쓸 수 없다 — 아직 묶기 전이라 집계값이 없음.)
- 실행 순서로 외우면 쉽다: **FROM/JOIN → WHERE(행 필터) → GROUP BY(묶기) → HAVING(그룹 필터) → SELECT → ORDER BY.**

> ★ JPA 연결 — JPQL에도 `group by` / `having` / `count` 등이 그대로 있고, 복잡한 통계는 스프링 데이터 JPA의
> `@Query`나 QueryDSL로 작성한다. 집계의 SQL 개념은 여기서 그대로 쓰인다.

---

## 1-표. 표로 보는 각 JOIN — 원본 테이블과 결과

말로만 보면 헷갈리니, **'조인 전 원본 테이블'과 '조인 후 결과 테이블'을 나란히** 두고 본다. 아래 데이터는
실습 코드(`JoinDemo`)와 동일하다.

### 원본 테이블 (조인 전)
`department` (부서)

| id | name |
|---|---|
| 1 | 개발 |
| 2 | 영업 |

`employee` (직원) — `dept_id`가 부서를 가리키는 외래 키. 박무소속은 부서가 없어 NULL.

| id | name | dept_id |
|---|---|---|
| 1 | 김개발 | 1 |
| 2 | 이영업 | 2 |
| 3 | 박무소속 | NULL |

### ① INNER JOIN — 양쪽 매칭만(교집합)
`employee e INNER JOIN department d ON e.dept_id = d.id`
- 각 직원의 `dept_id`로 부서를 찾아 붙이되, **매칭되는 행만** 남긴다. 박무소속은 `dept_id`가 NULL이라 가리킬
  부서가 없으므로 **결과에서 빠진다.**

| emp | dept | 설명 |
|---|---|---|
| 김개발 | 개발 | dept_id=1 → 개발 매칭 |
| 이영업 | 영업 | dept_id=2 → 영업 매칭 |
| ~~박무소속~~ | — | dept_id=NULL → 매칭 실패로 **제외** |

→ 원본 직원 3명 중 **2건**만 남음(매칭된 것만).

### ② LEFT JOIN — 왼쪽(직원) 전부 + 매칭(없으면 NULL)
`employee e LEFT JOIN department d ON e.dept_id = d.id`
- 왼쪽 테이블(employee)의 **모든 행을 유지**하고, 매칭되는 부서를 붙인다. 매칭이 없으면 부서 칸을 **NULL**로 채운다.

| emp | dept | 설명 |
|---|---|---|
| 김개발 | 개발 | 매칭됨 |
| 이영업 | 영업 | 매칭됨 |
| 박무소속 | **NULL** | 매칭 없지만 왼쪽이라 **남고**, 부서는 NULL |

→ 직원 **전부 3건**. INNER와의 차이는 딱 한 줄, 박무소속이 NULL로 살아남는 것.

### ③ 부서 없는 직원 — LEFT JOIN + WHERE d.id IS NULL
②번 LEFT JOIN 결과에서 **부서 쪽이 NULL인 행만** 거른다. "매칭 안 된 것만 골라내기" 패턴.

| emp | (조건) |
|---|---|
| 박무소속 | d.id IS NULL 인 행만 |

→ **1건**(박무소속). ②의 NULL 행만 추려낸 것.

### ④ CROSS JOIN — 카테시안 곱(모든 조합)
`employee e CROSS JOIN department d` — ON이 **없다**. 그래서 거르지 않고 **모든 조합**을 만든다(직원 3 × 부서 2 = 6행).

| emp | dept |
|---|---|
| 김개발 | 개발 |
| 김개발 | 영업 |
| 이영업 | 개발 |
| 이영업 | 영업 |
| 박무소속 | 개발 |
| 박무소속 | 영업 |

→ **6건**. INNER JOIN은 이 6개 중 `e.dept_id = d.id`인 2개만 남긴 것 = **CROSS JOIN + ON 필터**. (실무에서 ON을
빠뜨리면 이렇게 N×M으로 폭발한다.)

### ⑤ 1:N JOIN — '1'쪽 행 뻥튀기
개발 부서에 **직원 2명**(김개발, 최개발)을 두고, **부서 기준**으로 직원을 조인한다.
`department d JOIN employee e ON e.dept_id = d.id`

이때 employee는 (최개발 추가 후):

| id | name | dept_id |
|---|---|---|
| 1 | 김개발 | 1 |
| 4 | 최개발 | 1 |
| 2 | 이영업 | 2 |

결과 — '개발'이 직원 수만큼 **중복**된다:

| dept | emp | 설명 |
|---|---|---|
| 개발 | 김개발 | 개발의 1번째 직원 |
| 개발 | 최개발 | 개발의 2번째 직원 → '개발'이 **2번째 등장**(뻥튀기) |
| 영업 | 이영업 | 영업 직원 |

→ 직원까지 보려면 자연스럽지만, **부서 목록만** 원하면 '개발'이 2번이라 거슬린다.

### ⑥ DISTINCT로 중복 제거
⑤에서 `SELECT DISTINCT d.name` 하면 중복된 부서가 합쳐진다.

| dept |
|---|
| 개발 |
| 영업 |

→ **2건**. (PART 14 JPA 컬렉션 페치 조인에서 엔티티가 중복될 때 JPQL `distinct`로 푸는 것과 같은 원리.)

### ⑦ N:M JOIN — 중간 테이블로 세 테이블 연결
원본 세 테이블:

`student`

| id | name |
|---|---|
| 1 | 학생A |
| 2 | 학생B |

`course`

| id | title |
|---|---|
| 10 | 자바 |
| 20 | DB |

`enrollment` (중간 테이블 — 누가 무엇을 듣는지 '쌍'으로 저장)

| student_id | course_id |
|---|---|
| 1 | 10 |
| 1 | 20 |
| 2 | 10 |

`student s JOIN enrollment en ON en.student_id = s.id JOIN course c ON c.id = en.course_id` 결과:

| student | course | 설명 |
|---|---|---|
| 학생A | 자바 | (1,10) 쌍 |
| 학생A | DB | (1,20) 쌍 → 학생A는 2과목 |
| 학생B | 자바 | (2,10) 쌍 |

→ 중간 테이블의 **쌍 3개가 그대로 3행**이 된다. 다대다를 1:N(student→enrollment) + N:1(enrollment→course)로 풀어 이은 것.

### ⑧ UNION vs UNION ALL — 세로로 합치기
조회A = `개발팀 직원`(김개발·최개발), 조회B = `이름에 '개발' 포함`(김개발·최개발). 두 결과가 겹친다.

UNION (중복 제거):

| name |
|---|
| 김개발 |
| 최개발 |

UNION ALL (중복 유지):

| name | 설명 |
|---|---|
| 김개발 | 조회A에서 |
| 김개발 | 조회B에서 (중복) |
| 최개발 | 조회A에서 |
| 최개발 | 조회B에서 (중복) |

→ UNION은 **2건**, UNION ALL은 **4건**. JOIN(가로)과 달리 행을 **세로로** 쌓는다.

### ⑨ FULL OUTER 흉내 — LEFT UNION (반대방향) LEFT
직원 없는 부서 '인사'를 추가한 뒤, '직원 기준 LEFT' UNION '부서 기준 LEFT'를 하면 양쪽 미매칭이 다 나온다.

| emp | dept | 설명 |
|---|---|---|
| 김개발 | 개발 | 매칭 |
| 최개발 | 개발 | 매칭 |
| 이영업 | 영업 | 매칭 |
| 박무소속 | **NULL** | 부서 없는 직원(첫 쿼리에서) |
| **NULL** | 인사 | 직원 없는 부서(둘째 쿼리에서) |

→ 박무소속(dept NULL)과 인사(emp NULL)가 **둘 다** 보인다 = FULL OUTER 효과.

### ⑩ GROUP BY — 그룹별 집계
`department LEFT JOIN employee` 후 부서 이름으로 묶어 `count(e.id)`로 인원수를 센다.

| dept | cnt | 설명 |
|---|---|---|
| 개발 | 2 | 김개발·최개발 |
| 영업 | 1 | 이영업 |
| 인사 | 0 | 직원 없음(LEFT라 포함, count(e.id)라 NULL은 0) |

여기에 `HAVING count(e.id) >= 2`를 걸면 → 그룹을 거른다:

| dept | cnt |
|---|---|
| 개발 | 2 |

→ '개발'만 남는다(직원 2명 이상). WHERE가 아니라 HAVING이어야 집계 조건을 걸 수 있다.

---

## 2. 실습으로 확인하기

> - **가설**: 같은 두 테이블에 INNER/LEFT 조인을 하면 결과 행 수가 다르고(매칭 안 되는 직원 포함 여부),
>   LEFT+WHERE NULL로 '부서 없는 직원'만 골라낼 수 있다.
> - **가설(추가)**: CROSS JOIN은 모든 조합(3×2=6행), 1:N 조인은 부서가 직원 수만큼 중복(DISTINCT로 제거),
>   N:M은 중간 테이블로 세 테이블을 이어 조회된다.

### 코드 (`com.study.part11_jpa.s01_join`)
- `JoinDemo` — department/employee(박무소속은 dept_id NULL)에 INNER/LEFT/부서없는직원, 그리고 CROSS JOIN
  (카테시안 곱), 1:N 행 뻥튀기+DISTINCT, N:M(student-enrollment-course) 쿼리까지 실행.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part11_jpa.s01_join.JoinDemo
```

### 실행 결과 — 가설과 실제 비교
```
== INNER JOIN (양쪽 매칭만 = 박무소속 제외) ==
  EMP=김개발  DEPT=개발 / EMP=이영업  DEPT=영업                  ← 박무소속 빠짐(2건)
== LEFT JOIN (직원 전부 + 부서, 없으면 NULL) ==
  EMP=김개발  DEPT=개발 / EMP=이영업  DEPT=영업 / EMP=박무소속  DEPT=NULL   ← 전부(3건)
== 부서 없는 직원 (LEFT JOIN + WHERE d.id IS NULL) ==
  EMP=박무소속                                                  ← 매칭 안 된 행만
```
- INNER는 매칭(부서 있는)만 2건, LEFT는 직원 전부 3건(박무소속은 DEPT=NULL). ✅
- LEFT + WHERE NULL로 부서 없는 직원만 골라냄. ✅

추가 케이스 실측:
```
== CROSS JOIN (직원3 x 부서2 = 6행) ==        <- 모든 조합(거르기 전)
  김개발|개발, 김개발|영업, 이영업|개발, 이영업|영업, 박무소속|개발, 박무소속|영업
== 1:N JOIN 행 뻥튀기 ==                       <- 개발 부서 직원 2명이라 '개발'이 2번
  개발|김개발, 개발|최개발, 영업|이영업
== DISTINCT로 부서 목록 중복 제거 ==          -> 개발, 영업 (2건)
== N:M JOIN (학생-enrollment-과목) ==
  학생A|자바, 학생A|DB, 학생B|자바
```
- CROSS=카테시안 곱(6행), 1:N은 '개발' 중복→DISTINCT로 2건, N:M은 중간 테이블로 세 테이블 연결. ✅

---

## 3. 자기 점검

- **Q. 왜 정보를 여러 테이블로 나누고 JOIN으로 합치나?**
  - 내 답: 정규화로 중복을 없애기 위해. 그 결과 정보가 흩어지므로 함께 보려면 FK로 연결해 JOIN한다.

- **Q. INNER JOIN과 LEFT JOIN의 결과 차이는?**
  - 내 답: INNER는 양쪽 매칭만(교집합), LEFT는 왼쪽 전부 + 매칭(없으면 NULL). 그래서 매칭 안 되는 왼쪽
    행(부서 없는 직원)이 INNER에선 빠지고 LEFT엔 NULL로 남는다.

- **Q. "부서 없는 직원"을 어떻게 찾나?**
  - 내 답: LEFT JOIN 후 부서 쪽 컬럼이 NULL인 행만(`WHERE d.id IS NULL`). 매칭 안 된 행을 골라내는 패턴.

- **Q. JOIN의 본질은 무엇이고, ON을 빠뜨리면 왜 위험한가?**
  - 내 답: 두 테이블의 모든 행 조합(카테시안 곱=CROSS JOIN)을 만든 뒤 ON으로 거르는 것. ON을 빠뜨리면
    필터가 없어 N×M으로 행이 폭발한다(큰 테이블이면 DB 마비). INNER JOIN = CROSS JOIN + ON 필터.

- **Q. 1:N 조인에서 행이 중복되는 이유와 해결은?**
  - 내 답: 부서(1)에 직원(N)이 매달리면 부서가 직원 수만큼 중복된다(개발 직원 2명이면 '개발'이 2번).
    부서 목록만 원하면 DISTINCT로 제거. 이것이 JPA 컬렉션 페치 조인에서 엔티티가 중복되는 문제의 뿌리(distinct로 해결, PART 14).

- **Q. GROUP BY는 무엇이고, WHERE와 HAVING의 차이는?**
  - 내 답: 같은 값끼리 행을 묶어 그룹마다 집계(count/sum/avg 등)한다. WHERE는 묶기 전 개별 행을 거르고,
    HAVING은 묶은 뒤 그룹(집계 결과)을 거른다. 집계 함수 조건(count>=2 등)은 HAVING에만 쓸 수 있다.
    실행 순서: FROM/JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY.

- **Q. JOIN + GROUP BY로 부서별 직원 수를 셀 때 count(*)와 count(e.id) 차이는?**
  - 내 답: LEFT JOIN에서 직원 0명 부서는 e가 전부 NULL인 한 행으로 남는다. count(*)는 그 행도 1로 세어
    틀리고(인사=1), count(e.id)는 NULL을 안 세서 정확히 0이 나온다(인사=0).

- **Q. JOIN과 UNION의 차이는?**
  - 내 답: JOIN은 두 테이블을 '가로로'(한 행에 양쪽 컬럼) 합치고, UNION은 두 조회 결과를 '세로로'(행을 이어)
    합친다. UNION은 컬럼 수·타입이 같아야 하며, UNION=중복 제거 / UNION ALL=중복 유지(더 빠름). FULL OUTER
    미지원 DB(MySQL)에서 'LEFT UNION 반대방향 LEFT'로 FULL OUTER를 흉내 내는 게 대표 활용.

- **Q. 다대다(N:M)는 DB에서 어떻게 표현하나?**
  - 내 답: 관계형 DB는 다대다를 직접 못 만들어, 중간(조인) 테이블에 (A,B) 쌍을 저장해 1:N+N:1로 푼다.
    조회는 세 테이블을 이어 JOIN. JPA @ManyToMany/중간 엔티티의 토대(부가 컬럼이 필요하면 중간 엔티티 권장).

- **Q. (예고) JPA에서는 이 JOIN을 어떻게 다루나?**
  - 내 답: 엔티티의 연관관계(`@ManyToOne` 등)를 선언하면 JPA가 필요한 JOIN SQL을 자동 생성한다(11.3~). JOIN의
    개념 자체는 그대로 깔려 있다. 1:N 페치 시 중복(distinct)·다대다(중간 엔티티)도 이 JOIN 성질에서 나온다.
