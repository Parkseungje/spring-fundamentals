# PART 10 — DB 접근의 진화: 10.3 DataSource 추상화

> 이 문서는 커리큘럼 PART 10의 소단원 중 **10.3 DataSource 추상화**를 다룬다.
> 10.2에서 본 커넥션 풀(HikariCP) 같은 '연결 공급자'를, 표준 인터페이스 `javax.sql.DataSource`로 추상화해
> 구현을 갈아끼워도 코드가 안 바뀌게 한다. (PART 8.4 ConnectionMaker와 같은 DIP 사상)

---

## 0. 들어가기 전에 — 핵심 용어
- **DataSource(`javax.sql.DataSource`)**: '연결 공급자'의 표준 인터페이스. 핵심 메서드는 `getConnection()` 하나.
- **구현체**: `HikariDataSource`(풀), `BasicDataSource`(DBCP2), `DriverManagerDataSource`(풀 없는 어댑터) 등.
- **DriverManagerDataSource**: DriverManager를 DataSource 모양으로 감싼 어댑터. 풀이 없어 매번 새 연결 → **테스트·학습용(프로덕션 금지)**.
- **DIP(의존 역전)**: 구체 클래스가 아니라 추상(인터페이스)에 의존하라. 여기선 DAO가 DataSource(추상)에 의존.
- **`@Autowired`**: Spring이 빈을 자동 주입하는 어노테이션(여기선 DataSource 구현 빈을 주입).

한 줄 그림: **DAO가 구체 구현(HikariDataSource)이 아니라 `DataSource` 인터페이스에만 의존하면, 구현을
HikariCP↔DBCP2↔DriverManager로 바꿔도 DAO 코드는 그대로다(설정만 변경). = DIP의 실현.**

---

## 1. 학습 내용 — '연결 공급자'를 인터페이스로

### 로우레벨의 불편함 — 구현마다 사용법이 다르면
연결을 얻는 방법이 `DriverManager`, HikariCP, DBCP2마다 **사용법이 다르면**, 풀 구현을 바꿀 때 그걸 쓰는
애플리케이션 코드가 전부 바뀐다. "연결 얻기" 하나 바꾸려다 DAO를 다 수정하는 셈.

### 해결 — javax.sql.DataSource 표준 인터페이스
모든 연결 공급자가 `DataSource`라는 표준 인터페이스를 구현하게 한다. 사용하는 쪽은 **`getConnection()`
하나만** 알면 된다.
```java
public class CustomerDao {
    private final DataSource dataSource;            // 구체 구현이 아니라 '인터페이스'에 의존
    public CustomerDao(DataSource ds) { this.dataSource = ds; }   // 구현은 외부에서 주입
    public void add(Customer c) {
        try (Connection conn = dataSource.getConnection()) { ... }  // 어떤 구현이든 동일
    }
}
```
- 구현 교체: HikariDataSource → DBCP2(BasicDataSource) → ... **설정만** 바꾸면 되고 **DAO 코드는 불변**.
- 이것이 **DIP**다. PART 8.4의 `ConnectionMaker` 인터페이스와 정확히 같은 사상이고, 자바 표준이 그걸
  `DataSource`로 제공하는 것뿐이다.
- Spring에선 보통 `@Autowired DataSource dataSource;`로 풀 구현 빈을 주입받는다(어떤 구현인지 몰라도 됨).

### 구현체 두 가지 성격
- **HikariDataSource**: 진짜 커넥션 풀(재사용, 10.2). **프로덕션용**.
- **DriverManagerDataSource**: DriverManager를 DataSource로 감싼 **어댑터**. 풀이 없어 `getConnection()`마다
  새 연결을 만든다 → **테스트·학습용**. 프로덕션에 쓰면 10.2의 비효율(매번 새 연결)을 그대로 떠안는다.

---

## 2. 실습으로 확인하기

> - **가설**: 같은 CustomerDao에 HikariDataSource를 주든 DriverManagerDataSource를 주든, DAO 코드 수정
>   없이 둘 다 동작한다(DataSource 표준 = DIP).

### 코드 (`com.study.part10_db.s03_datasource`)
- `CustomerDao` — `DataSource` 인터페이스에만 의존(생성자 주입), `getConnection()`으로 add/get.
- `Main` — HikariDataSource / DriverManagerDataSource 두 구현을 같은 DAO에 주입해 비교.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s03_datasource.Main
```

### 실행 결과 — 가설과 실제 비교
```
[HikariDataSource(풀)] Customer{id=h1, name=HikariDataSource(풀), age=30}
[DriverManagerDataSource(풀 없음)] Customer{id=d1, name=DriverManagerDataSource(풀 없음), age=30}
=> CustomerDao 코드는 그대로, 주입하는 DataSource 구현만 교체했다. 'getConnection()' 표준(DIP) 덕분.
```
- 두 DataSource 구현 모두에서 **같은 CustomerDao**가 정상 동작. ✅ → 구현 교체에도 DAO 코드 수정 0(DIP).
- (HikariCP는 시작 시 풀을 만든다는 로그가 보이고, DriverManagerDataSource는 풀 없이 동작 — 학습용.)

---

## 3. 자기 점검

- **Q. DataSource 인터페이스의 핵심 메서드와 그 의미는?**
  - 내 답: `getConnection()` 하나. "연결을 어디서 어떻게 만들든(풀이든 아니든) 호출하는 쪽은 이거 하나만
    알면 된다"는 표준 — 구현 교체에 코드가 안 바뀌게 해준다(DIP).

- **Q. DataSource 추상화는 PART 8의 무엇과 같은 사상인가?**
  - 내 답: PART 8.4의 ConnectionMaker 인터페이스 + DIP. '구체 구현이 아니라 추상에 의존'하고 구현을
    외부에서 주입. 자바 표준이 이를 DataSource로 제공한 것.

- **Q. DriverManagerDataSource를 프로덕션에 쓰면 안 되는 이유는?**
  - 내 답: 풀이 없어 getConnection마다 새 연결을 만든다(10.2의 비효율 그대로). 테스트·학습용이고, 운영은
    HikariCP 같은 풀 구현을 써야 한다.
