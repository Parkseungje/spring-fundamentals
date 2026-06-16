# PART 10 — DB 접근의 진화: 10.1 JDBC 표준화

> 이 문서는 커리큘럼 PART 10의 소단원 중 **10.1 JDBC 표준화**를 다룬다.
> PART 8이 'DAO 객체 설계'의 진화였다면, PART 10은 'DB 접근 코드' 자체의 추상화 여정이다. 그 출발점인
> 10.1은 "DB마다 제각각이던 접근 방식을 하나의 표준 API로 통일한" JDBC를 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **JDBC(Java Database Connectivity)**: 자바의 'DB 접근 표준 API'. 어떤 DB든 같은 방식(인터페이스)으로 다루게 함.
- **드라이버(driver)**: 특정 DB(H2/MySQL/Oracle...)와 실제로 통신하는 구현체. JDBC 표준을 그 DB에 맞게 구현한 것.
- **Connection**: DB와의 연결. **PreparedStatement**: SQL + 파라미터 바인딩(`?`). **ResultSet**: 조회 결과(행 집합).
- **DriverManager**: URL을 보고 알맞은 드라이버로 Connection을 만들어 주는 표준 진입점.
- **SQL 방언(dialect)**: DB마다 다른 SQL 문법(페이징 LIMIT vs ROWNUM, 자동증가 AUTO_INCREMENT vs SEQUENCE 등).

한 줄 그림: **JDBC는 "DB가 달라도 같은 코드(Connection/PreparedStatement/ResultSet)로 접근"하게 해주는
표준 API다. DB 교체 시 URL·드라이버만 바꾸면 된다. 단 SQL 문법 차이까지는 못 풀어준다(상위 추상화의 동기).**

---

## 1. 학습 내용 — DB마다 제각각 → 하나의 표준

### JDBC 없던 시절의 고통
- DB마다 **고유 API**가 달랐다. DB를 바꾸면 접근 코드를 전부 다시 짜야 했다.
- 심하면 **소켓으로 DB의 바이너리 프로토콜을 직접** 다뤘다.
  ```java
  Socket socket = new Socket("localhost", 3306);  // MySQL 프로토콜을 손으로...
  ```
  → DB 접근이 매번 저수준 통신부터 시작 = 이식성 0, 생산성 최악.

### JDBC = 자바의 DB 접근 표준 API
JDBC는 연결·SQL 실행·결과 처리 방식을 **표준 인터페이스로 통일**했다.
- 핵심 타입: `Connection`(연결) → `PreparedStatement`(SQL+바인딩) → `ResultSet`(결과).
- **DB를 바꿔도 URL과 드라이버만 변경**하면 나머지 코드는 그대로. (PART 8.4의 인터페이스/전략 패턴 사상이
  DB 접근에 적용된 것 — '구체 DB가 아니라 표준 API에 의존' = DIP.)
```java
Class.forName("org.h2.Driver");                          // 드라이버 (DB 바뀌면 여기와 url만)
Connection c = DriverManager.getConnection(url, ...);   // 표준 진입점
// 이후 PreparedStatement, ResultSet 코드는 어떤 DB든 동일
```

### JDBC가 해결 '못' 하는 것 — SQL 방언
JDBC는 'API'를 표준화했지 'SQL 문법'을 표준화하진 못한다.
- 페이징: MySQL `LIMIT 10` vs Oracle `ROWNUM <= 10`
- 자동 증가: MySQL `AUTO_INCREMENT` vs Oracle `SEQUENCE`

이 방언 차이는 JDBC로도 남는다. 그래서 더 위에서 이를 흡수하려는 상위 프레임워크(**JdbcTemplate**(10.5),
**JPA/Hibernate**(PART 11), MyBatis)가 등장한다. 즉 PART 10/11은 "JDBC 위에 무엇을 더 얹어 편하게 만드나"의 이야기다.

---

## 2. 실습으로 확인하기

> - **가설**: 'URL'만 다른 두 연결에 대해 '같은 JDBC 코드'가 그대로 동작한다(표준 API라서 DB에 무관).

### 코드 (`com.study.part10_db.s01_jdbc_standard`)
- `JdbcStandardDemo` — DB에 무관한 `runWith(url)`(표준 JDBC로 insert/select)를 URL만 다른 두 연결에 실행.
- `BeforeAfterJdbc` — JDBC '이전'(벤더별 고유 API, 메서드 이름까지 제각각) vs '이후'(표준 API) 코드 비교.
- `DialectDifference` — JDBC가 못 없애는 'SQL 방언 차이'를 코드로 명시(LIMIT vs ROWNUM, AUTO_INCREMENT vs SEQUENCE).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s01_jdbc_standard.JdbcStandardDemo
./gradlew runStage -Pmain=com.study.part10_db.s01_jdbc_standard.BeforeAfterJdbc
./gradlew runStage -Pmain=com.study.part10_db.s01_jdbc_standard.DialectDifference
```

### BeforeAfterJdbc / DialectDifference 핵심 출력
```
[JDBC 이전] MySQL: connectMySql/runQuery   Oracle: openOracle/fetchRows  <- API 이름부터 달라 DB 교체=재작성
[JDBC 이후] 둘 다 getConnection/executeQuery 로 통일 (URL/드라이버만 변경)

[페이징] MySQL/H2: ... LIMIT 2     Oracle: ... where ROWNUM <= 2   <- SQL 문법 다름(JDBC가 못 없앰)
[자동증가] MySQL: AUTO_INCREMENT    Oracle: SEQUENCE
```

### 실행 결과 — 가설과 실제 비교
```
== 연결 A (mem:part10_a) ==
  조회 결과 = Customer{id=c1, name=표준회원, age=30}
== 연결 B (mem:part10_b) ==
  조회 결과 = Customer{id=c1, name=표준회원, age=30}
```
- 두 연결(URL만 다름)에서 **같은 runWith 코드**가 그대로 동작. ✅ → JDBC가 표준 API라 DB(연결)에 무관함을 확인.
- 단, SQL 문법 차이는 JDBC가 못 풀어준다(방언) → 10.5/PART 11에서 상위 추상화로 이어짐.

---

## 3. 자기 점검

- **Q. JDBC가 표준화한 것과 표준화하지 못한 것은?**
  - 내 답: 표준화한 것 = DB 접근 'API'(Connection/PreparedStatement/ResultSet)와 연결 방식(URL+드라이버).
    못한 것 = SQL '문법 차이'(LIMIT vs ROWNUM 등 방언). 방언은 상위 프레임워크가 흡수한다.

- **Q. DB를 MySQL에서 Oracle로 바꾸면 JDBC 코드에서 무엇만 바뀌나?**
  - 내 답: URL과 드라이버. PreparedStatement/ResultSet을 쓰는 코드 자체는 그대로(SQL 방언만 손보면 됨).

- **Q. JDBC 표준화는 PART 8의 어떤 원칙과 같은 사상인가?**
  - 내 답: '구체 DB가 아니라 표준 API(추상)에 의존' = DIP, 그리고 구현(드라이버)을 갈아끼우는 전략 패턴 사상.
    (PART 8.4 ConnectionMaker 인터페이스와 같은 아이디어.)
