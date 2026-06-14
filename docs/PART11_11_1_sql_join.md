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

한 줄 그림: **정규화로 정보가 여러 테이블에 나뉘면, JOIN으로 다시 합친다. INNER=양쪽 매칭만(교집합),
LEFT=왼쪽 전부(없으면 NULL). 'LEFT + WHERE NULL'로 매칭 안 되는 행을 찾는다.**

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

---

## 2. 실습으로 확인하기

> - **가설**: 같은 두 테이블에 INNER/LEFT 조인을 하면 결과 행 수가 다르고(매칭 안 되는 직원 포함 여부),
>   LEFT+WHERE NULL로 '부서 없는 직원'만 골라낼 수 있다.

### 코드 (`com.study.part11_jpa.s01_join`)
- `JoinDemo` — department/employee 두 테이블(박무소속은 dept_id NULL)에 INNER/LEFT/부서없는직원 쿼리 실행.

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

---

## 3. 자기 점검

- **Q. 왜 정보를 여러 테이블로 나누고 JOIN으로 합치나?**
  - 내 답: 정규화로 중복을 없애기 위해. 그 결과 정보가 흩어지므로 함께 보려면 FK로 연결해 JOIN한다.

- **Q. INNER JOIN과 LEFT JOIN의 결과 차이는?**
  - 내 답: INNER는 양쪽 매칭만(교집합), LEFT는 왼쪽 전부 + 매칭(없으면 NULL). 그래서 매칭 안 되는 왼쪽
    행(부서 없는 직원)이 INNER에선 빠지고 LEFT엔 NULL로 남는다.

- **Q. "부서 없는 직원"을 어떻게 찾나?**
  - 내 답: LEFT JOIN 후 부서 쪽 컬럼이 NULL인 행만(`WHERE d.id IS NULL`). 매칭 안 된 행을 골라내는 패턴.

- **Q. (예고) JPA에서는 이 JOIN을 어떻게 다루나?**
  - 내 답: 엔티티의 연관관계(`@ManyToOne` 등)를 선언하면 JPA가 필요한 JOIN SQL을 자동 생성한다(11.3~). JOIN의
    개념 자체는 그대로 깔려 있다.
