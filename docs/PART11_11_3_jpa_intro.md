# PART 11 — ORM/JPA와 트랜잭션 추상화: 11.3 JPA 입문

> 이 문서는 커리큘럼 PART 11의 소단원 중 **11.3 JPA 입문**을 다룬다.
> 11.2에서 본 객체-관계 미스매치를 자동으로 메우는 ORM의 자바 표준이 JPA다. SQL을 직접 쓰던 JdbcTemplate
> (10.5)에서 한 단계 더 올라가, "SQL을 JPA가 대신 써 주는" 세계로 들어간다.

---

## 0. 들어가기 전에 — 핵심 용어
- **JPA(Java Persistence API)**: 자바 ORM '표준 인터페이스'. 어노테이션으로 매핑하면 SQL을 대신 생성해 준다.
- **Hibernate**: JPA의 대표 '구현체'. (JPA=인터페이스, Hibernate=실제 동작) Spring Boot 기본.
- **@Entity**: "이 클래스를 DB 테이블과 매핑되는 객체로 관리하라"는 표시.
- **@Id / @GeneratedValue**: 기본 키(PK) 필드 / PK 자동 생성 전략(IDENTITY=auto_increment 등).
- **EntityManager**: JPA가 엔티티를 저장·조회·관리하는 핵심 객체(내부적으로 SQL 생성·실행).
- **영속성 컨텍스트(persistence context)**: EntityManager가 엔티티를 보관·관리하는 '1차 캐시' 공간. 트랜잭션 단위로 살아 있다.
- **변경 감지(Dirty Checking)**: 영속 상태 엔티티의 필드가 바뀌면, save 호출 없이도 커밋/flush 시 update SQL을 자동 생성하는 기능.
- **JPQL**: 테이블이 아니라 '엔티티와 그 필드'를 대상으로 쓰는 객체 지향 쿼리(JPA가 SQL로 번역). 복잡 쿼리는 `@Query`로 직접 작성.
- **Spring Data JPA**: JPA를 더 쉽게 — `Repository` 인터페이스만 선언하면 구현을 자동 생성. 쿼리 메서드(findByName) 지원.
- **쿼리 메서드(query method)**: 메서드 이름(`findByName`) 규칙으로 SQL(JPQL)을 자동 생성하는 기능.

한 줄 그림: **@Entity로 객체-테이블 매핑을 '선언'만 하면, JPA(Hibernate)가 insert/select SQL을 대신 만든다.
Spring Data JPA는 리포지토리 인터페이스만 만들면 구현까지 자동 — 'findByName' 같은 이름으로 쿼리가 생성된다.**

---

## 1. 학습 내용 — SQL을 대신 써 주는 ORM

### SQL Mapper(JdbcTemplate/MyBatis)의 한계
10.5의 JdbcTemplate은 자원 정리·매핑은 자동화했지만, **SQL은 여전히 개발자가 직접** 썼고 객체 그래프도
수동이었다(11.2). DB가 바뀌면 SQL 방언도 손봐야 했다.

### JPA = 자바 ORM 표준
JPA는 **어노테이션으로 매핑을 선언**하면 SQL을 대신 생성한다.

| | SQL Mapper(JdbcTemplate) | JPA |
|---|---|---|
| SQL | 개발자가 직접 작성 | 자동 생성 |
| 결과 매핑 | RowMapper 수동 | 어노테이션 선언 |
| 객체 그래프 | 수동 조립 | 자동(Lazy Loading) |
| 학습곡선 | 낮음 | 높음 |

- **동작 위치**: Application → **JPA(Hibernate)** → JDBC → DB. JPA도 결국 내부에서 JDBC·DataSource·HikariCP를
  그대로 쓴다(10장 위에 얹힌 한 겹). 즉 JPA가 JDBC를 대체하는 게 아니라 그 위에서 SQL을 자동 생성하는 것.

### @Entity + Spring Data JPA
```java
@Entity
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    private int price;
}

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByName(String name);   // 메서드 이름만으로 'where name = ?' 자동 생성
}
```
- `@Entity` 선언 → JPA가 Item ↔ item 테이블 매핑을 관리, insert/select SQL을 자동 생성.
- `JpaRepository` 상속 인터페이스만 만들면 → Spring Data JPA가 **구현체를 런타임에 자동 생성·빈 등록**.
  `save/findById/findAll/delete` 등을 구현 없이 사용. **쿼리 메서드**(findByName)는 이름을 해석해 쿼리 생성.

### ★ 영속성 컨텍스트 — JPA의 심장 (1차 캐시·동일성)
JPA가 단순히 'SQL 자동 생성기'가 아닌 이유가 여기 있다. EntityManager는 엔티티를 **영속성 컨텍스트**라는
공간(1차 캐시)에 보관하며 관리한다. 입문에서 꼭 맛봐야 할 두 효과:
- **1차 캐시 + 동일성 보장**: 같은 트랜잭션 안에서 **같은 PK를 두 번 조회하면, DB를 다시 치지 않고 캐시에
  보관된 '같은 인스턴스'를 돌려준다**. 그래서 자바 `==`도 **true**다.
  - ★ 이것이 11.2에서 약속한 식별 미스매치의 해결이다. raw JDBC는 `new`를 두 번 해 `==` false였지만(11.2 B),
    JPA는 영속성 컨텍스트 덕분에 `==` true. (실측: 두 번째 findById는 select 로그가 안 찍힌다 = 캐시에서 반환.)
- (생명주기·쓰기 지연·플러시 시점 등 깊은 내용은 **PART 14(JPA 심화)** 에서 본격적으로 다룬다. 여기선 맛보기.)

### ★ 변경 감지(Dirty Checking) — "update를 안 썼는데 반영된다"
JPA 입문의 대표적 "마법". **영속 상태(영속성 컨텍스트가 관리 중인) 엔티티의 필드만 바꾸면**, `save()`나
update SQL을 호출하지 않아도 트랜잭션 커밋(또는 flush) 시 JPA가 **변경을 감지해 update SQL을 자동 생성·실행**한다.
```java
Item item = itemRepository.findById(id).orElseThrow(); // 영속 상태
item.changePrice(9900);                                 // 값만 바꿈 (save 호출 안 함!)
// 트랜잭션 커밋/flush 시점에 JPA가 알아서 update item set price=9900 ... 실행
```
- 원리: 영속성 컨텍스트가 엔티티를 처음 읽을 때 '스냅샷'을 떠 두고, flush 시점에 현재 값과 비교해 달라진
  필드를 찾아 update를 만든다. (그래서 변경 감지는 '영속 상태'에서만 작동한다 — 자세한 상태는 PART 14.)

### JPA는 트랜잭션 안에서 동작한다
영속성 컨텍스트는 **트랜잭션 단위**로 살아 있다(보통 트랜잭션 시작~커밋). 그래서 위의 1차 캐시 동일성·변경
감지·지연 로딩은 모두 **`@Transactional` 범위 안에서** 일어난다. 트랜잭션이 끝나면 영속성 컨텍스트도 닫혀
관리가 끝난다(이때 지연 로딩을 시도하면 예외 — PART 14에서 다룸). 데이터를 바꾸는 작업은 트랜잭션이 필수다.

### 쿼리 메서드로 부족할 때 — JPQL / @Query
`findByName` 같은 쿼리 메서드는 편하지만, 조건이 복잡하면 메서드 이름이 감당이 안 된다. 그땐 **JPQL**을 직접 쓴다.
```java
@Query("select i from Item i where i.name = :name and i.price >= :minPrice")
List<Item> searchByNameAndMinPrice(@Param("name") String name, @Param("minPrice") int minPrice);
```
- JPQL은 **테이블이 아니라 '엔티티(Item)와 그 필드(name/price)'를 대상**으로 쓰는 객체 지향 쿼리다. JPA가 이를
  실제 SQL로 번역한다(`from Item i` → `from item ...`).
- 더 복잡·동적인 쿼리는 타입 안전한 **Querydsl**로 간다(아래).

### 생태계 (실무 표준 조합)
- **Spring Data JPA**: 위처럼 리포지토리 인터페이스 기반 자동화(CRUD + 쿼리 메서드).
- **JPQL/@Query**: 메서드 이름으로 부족한 정형 쿼리를 객체 지향 쿼리로 직접 작성.
- **Querydsl**: 타입 안전한 동적 쿼리(컴파일 시점 오류 검출). 복잡·동적 쿼리에 쓴다.
- 실무 표준: **Spring Data JPA + Querydsl** 조합. (단순 CRUD/정형은 Data JPA·JPQL, 복잡·동적은 Querydsl)

---

## 2. 실습으로 확인하기

> - **가설**: SQL을 한 줄도 안 쓰고도 save/findById/findByName이 동작한다(JPA가 SQL 자동 생성). 나아가
>   같은 PK 두 번 조회는 같은 인스턴스(`==` true, 1차 캐시)이고, 필드만 바꿔도 update가 자동 실행되며(변경 감지),
>   복잡 조건은 JPQL @Query로 처리된다.

### 코드 (`com.study.part11_jpa.s03_jpa_intro`)
- `Item` — `@Entity`로 매핑한 최소 엔티티(+ 변경 감지 데모용 `changePrice`).
- `ItemRepository` — `JpaRepository` 상속(+ 쿼리 메서드 findByName/findByPriceGreaterThanEqualOrderByPriceAsc,
  + JPQL `@Query` searchByNameAndMinPrice).
- 테스트 `JpaIntroTest`(`@DataJpaTest`, TestEntityManager로 flush/clear) — save/findById, 쿼리 메서드,
  1차 캐시 동일성, 변경 감지, JPQL을 SQL 없이 검증.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew test --tests "com.study.part11_jpa.s03_jpa_intro.*"
```
(application.yml에 `show-sql: true`가 켜져 있어, 실행 시 Hibernate가 생성한 insert/select SQL이 로그에 찍힌다.)

### 실행 결과 — 가설과 실제 비교
- `BUILD SUCCESSFUL` — 5개 테스트 모두 통과. ✅
  - saveAndFind: SQL 없이 save→findById 동작(IDENTITY로 PK 자동 채번).
  - queryMethod: `findByName("커피")`가 2건, 가격 조건 쿼리 메서드가 오름차순 정렬로 동작.
  - firstLevelCacheIdentity: 같은 PK 두 번 조회 → `isSameAs`(== true). 1차 캐시로 동일 인스턴스 반환(11.2 식별 미스매치 해소).
  - dirtyChecking: `changePrice`만 호출하고 save 안 했는데, flush/clear 후 재조회 시 9900으로 반영(update 자동 생성).
  - jpqlQuery: JPQL `@Query`로 "커피 && price>=5000" → 6000짜리 1건.
- **우리가 SQL을 한 줄도 안 썼는데 저장·조회·수정이 됐다는 것** 자체가 "JPA가 SQL을 대신 생성"의 증거.
  (show-sql 로그에서 insert/select/update를 눈으로도 확인 가능. 동일성 케이스에선 두 번째 select가 안 찍힌다 = 1차 캐시.)

---

## 3. 자기 점검

- **Q. JPA와 Hibernate의 관계는?**
  - 내 답: JPA는 자바 ORM '표준 인터페이스', Hibernate는 그 '구현체'. JPA로 코딩하고 실제 동작은 Hibernate가 한다.

- **Q. SQL Mapper(JdbcTemplate)와 JPA의 핵심 차이는?**
  - 내 답: JdbcTemplate은 SQL을 직접 쓰고 매핑(RowMapper)도 수동, JPA는 어노테이션 매핑으로 SQL을 자동 생성하고
    객체 그래프도 자동 로딩. 대신 학습곡선이 높다.

- **Q. Spring Data JPA의 쿼리 메서드란?**
  - 내 답: `findByName`처럼 메서드 이름 규칙만으로 SQL/JPQL을 자동 생성하는 기능. 구현 코드를 안 써도 된다.

- **Q. JPA를 쓰면 JDBC는 안 쓰이나?**
  - 내 답: 쓰인다. JPA(Hibernate)는 내부적으로 JDBC·DataSource·HikariCP를 그대로 사용한다. JPA는 그 위에
    얹혀 SQL을 자동 생성하는 한 겹일 뿐(Application→JPA→JDBC→DB).

- **Q. 영속성 컨텍스트(1차 캐시)가 무엇이고, 11.2와 어떻게 연결되나?**
  - 내 답: EntityManager가 엔티티를 보관·관리하는 트랜잭션 단위 공간. 같은 PK를 두 번 조회하면 DB 재조회
    없이 같은 인스턴스를 반환(== true). 11.2의 식별 미스매치(raw JDBC는 == false)를 이걸로 해소한다.

- **Q. 변경 감지(Dirty Checking)란?**
  - 내 답: 영속 상태 엔티티의 필드만 바꾸면 save 호출 없이도 커밋/flush 시 update SQL이 자동 생성·실행되는
    기능. 영속성 컨텍스트가 스냅샷과 비교해 달라진 필드를 찾아낸다. 영속 상태에서만 작동.

- **Q. 쿼리 메서드로 부족할 때는?**
  - 내 답: JPQL을 `@Query`로 직접 쓴다(엔티티·필드 대상의 객체 지향 쿼리 → JPA가 SQL로 번역). 더 복잡·동적이면
    타입 안전한 Querydsl. 실무는 Spring Data JPA + (JPQL) + Querydsl 조합.
