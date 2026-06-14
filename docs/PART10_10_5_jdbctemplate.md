# PART 10 — DB 접근의 진화: 10.5 JdbcTemplate

> 이 문서는 커리큘럼 PART 10의 마지막 소단원 **10.5 JdbcTemplate**을 다룬다.
> 10.1~10.4에서 직접 겪은 순수 JDBC의 '반복 코드'를 JdbcTemplate이 어떻게 없애는지 본다. PART 8의 설계
> 원칙(템플릿 메소드·전략 패턴·DI·람다)이 실제 Spring 클래스로 집약되는 지점이다.

---

## 0. 들어가기 전에 — 핵심 용어
- **JdbcTemplate**: 순수 JDBC의 반복(연결 획득/반환·Statement·ResultSet·예외 처리)을 대신해 주는 Spring 클래스.
- **RowMapper**: ResultSet의 '한 행'을 객체로 바꾸는 전략(함수형 인터페이스 → 람다 가능, PART 5).
- **BeanPropertyRowMapper**: 컬럼명 ↔ 필드명을 자동 매핑(snake_case ↔ camelCase)해 주는 기본 RowMapper.
- **주요 메서드**: `update`(INSERT/UPDATE/DELETE) · `queryForObject`(단일 행/값) · `query`(여러 행) · `execute`(DDL).
- **예외**: `queryForObject`가 0건이면 `EmptyResultDataAccessException`, 2건+면 `IncorrectResultSizeDataAccessException`.

한 줄 그림: **JdbcTemplate은 변하지 않는 흐름(연결·자원 정리·예외)을 숨기고, 개발자는 'SQL + 파라미터 +
결과 매핑(RowMapper)'만 넘긴다. 그래서 try/close/catch가 코드에서 사라진다. = PART 8 템플릿 메소드 패턴의 실제 구현.**

---

## 1. 학습 내용 — 반복을 숨기다

### 순수 JDBC의 고통 (10.1~10.4에서 직접 겪음)
매 쿼리마다 이런 코드가 반복됐다.
```java
try (Connection c = ds.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setString(1, ...);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) { ... 매핑 ... }
    }
} // SQLException 처리까지
```
- 자원 해제(close)·예외 처리 코드가 **본 로직(SQL·매핑)보다 길다.** close를 빠뜨리면 커넥션 누수.

### JdbcTemplate = 템플릿 메소드 + 전략 패턴의 실제 구현
- **변하지 않는 흐름**(연결 획득/반환, PreparedStatement 생성, ResultSet 순회, 예외 변환)은 JdbcTemplate이
  **숨겨서** 처리한다(템플릿 메소드 — PART 8.2/8.3).
- **변하는 부분**(SQL, 파라미터, 결과 매핑)만 개발자가 인자로 넘긴다(전략 — PART 8.4).
```java
JdbcTemplate jt = new JdbcTemplate(dataSource);   // DataSource(10.3)만 있으면 됨

jt.update("insert into customers(id,name,age) values (?,?,?)", id, name, age);   // 쓰기
Customer one = jt.queryForObject("select * from customers where id=?",
                                 new BeanPropertyRowMapper<>(Customer.class), id); // 단일 행
List<Customer> list = jt.query("select * from customers where age>=?",
                               (rs, n) -> new Customer(rs.getString("id"), rs.getString("name"), rs.getInt("age")),
                               minAge);            // 여러 행 (RowMapper 람다)
```
→ Connection/close/try-catch가 **코드에서 사라지고**, `SQL + 파라미터 + RowMapper`만 남는다.

### RowMapper — 결과를 객체로
- ResultSet 한 행 → 객체로 바꾸는 전략. 함수형 인터페이스라 **람다**로 쓸 수 있다(PART 5.4).
- `BeanPropertyRowMapper<>(Customer.class)`: 컬럼명과 필드명을 **자동 매핑**(snake↔camel). 직접 안 적어도 됨.

### 구조적 의미 — "좋은 라이브러리는 좋은 OOP 원칙의 집약체"
JdbcTemplate 한 줄에는 PART 1·5·8의 원칙이 다 들어 있다:
- 관심사 분리(흐름 vs SQL) + 템플릿 메소드(흐름 고정) + 전략 패턴(RowMapper 주입) + OCP/DIP(DataSource 의존)
  + DI(DataSource 주입) + 람다(RowMapper). → PART 8~10의 추상화 여정이 도착하는 곳이 JdbcTemplate이다.

---

## 2. 실습으로 확인하기

> - **가설**: JdbcTemplate으로 작성한 DAO에는 Connection/close/try-catch가 없고 SQL+매핑만 있다. update/
>   queryForObject/query가 정상 동작하고, BeanPropertyRowMapper/RowMapper 람다로 결과가 객체로 매핑된다.

### 코드 (`com.study.part10_db.s05_jdbctemplate`)
- `CustomerDao` — JdbcTemplate으로 createTable(execute)/add(update)/get(queryForObject+BeanPropertyRowMapper)/
  findByMinAge(query+RowMapper 람다)/count.
- `Main` — DAO를 실행해 동작 확인.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s05_jdbctemplate.Main
```

### 실행 결과 — 가설과 실제 비교
```
[10.5] count = 3
[10.5] get(c1) = Customer{id=c1, name=김삼십, age=30}  (BeanPropertyRowMapper 자동 매핑)
[10.5] age>=30 = [Customer{c1,김삼십,30}, Customer{c2,이사십,40}]  (RowMapper 람다)
=> Connection/close/try-catch가 코드에서 사라지고 'SQL + 파라미터 + RowMapper'만 남았다.
```
- update/queryForObject/query 모두 정상. 컬럼→필드 자동 매핑(BeanPropertyRowMapper)과 람다 매핑 확인. ✅
- DAO 코드에 자원 해제/예외 처리가 전혀 없다 → JdbcTemplate이 반복(템플릿)을 숨겼다.

---

## 3. PART 10 정리 — DB 접근의 추상화 여정
| 소단원 | 핵심 |
|---|---|
| 10.1 JDBC 표준화 | DB 접근 API 통일(URL·드라이버만 교체) |
| 10.2 Connection Pool | 연결 재사용으로 비용 제거(HikariCP) |
| 10.3 DataSource | 연결 공급자를 인터페이스로 추상화(DIP) |
| 10.4 트랜잭션·ACID | 작업을 묶어 안전하게(commit/rollback, 격리) |
| 10.5 JdbcTemplate | 반복 제거 — 템플릿 메소드+전략 패턴의 집약 |

---

## 4. 자기 점검

- **Q. JdbcTemplate이 없애 주는 '반복'은 구체적으로 무엇인가?**
  - 내 답: 연결 획득/반환, PreparedStatement 생성, ResultSet 순회, close, SQLException 처리. 개발자는
    SQL·파라미터·RowMapper만 넘기면 된다.

- **Q. JdbcTemplate은 PART 8의 어떤 패턴의 구현인가?**
  - 내 답: 템플릿 메소드 패턴(변하지 않는 흐름은 숨기고 변하는 부분만 주입) + 전략 패턴(RowMapper). 'Template'
    이름도 거기서 왔다.

- **Q. RowMapper와 BeanPropertyRowMapper의 차이는?**
  - 내 답: RowMapper는 ResultSet 한 행→객체 변환을 내가 직접(람다로) 작성. BeanPropertyRowMapper는 컬럼명↔
    필드명을 자동 매핑해 주는 기본 구현이라 직접 안 적어도 된다.

- **Q. queryForObject가 0건/2건일 때 각각 무슨 일이 일어나나?**
  - 내 답: 0건 → EmptyResultDataAccessException, 2건 이상 → IncorrectResultSizeDataAccessException. 단일 행을 기대하는 메서드라서.
