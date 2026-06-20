# PART 11 — ORM/JPA와 트랜잭션 추상화: 11.4 JPA 엔티티 매핑

> 이 문서는 커리큘럼 PART 11의 소단원 중 **11.4 JPA 엔티티 매핑**을 다룬다.
> 11.3에서 @Entity로 객체-테이블을 연결했다면, 11.4는 그 매핑을 '어떻게' 세밀하게 선언하는지
> (@Entity 조건, @Id/@GeneratedValue 전략, @Column, 네이밍 전략)를 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **@Entity**: JPA가 관리하는 객체임을 표시. 조건: **기본 생성자 필수**, **final 금지**(아래).
- **@Id**: 기본 키(PK) 필드. 반드시 1개(또는 복합 키).
- **@GeneratedValue(strategy)**: PK 자동 생성 전략(IDENTITY/SEQUENCE/TABLE/AUTO).
- **@Column**: 컬럼 세부 옵션(length·nullable·unique·columnDefinition).
- **@Table**: 테이블 단위 옵션(name·복합 유니크 uniqueConstraints·인덱스 indexes).
- **@Enumerated**: 자바 enum을 DB에 저장하는 방식 지정. STRING(이름) vs ORDINAL(순서 숫자).
- **@Embeddable / @Embedded**: 값 타입(식별자 없는 값 객체)을 엔티티 테이블 컬럼으로 펼쳐 매핑.
- **네이밍 전략(naming strategy)**: 필드명 ↔ 컬럼명 변환 규칙. Spring Boot 기본은 camelCase ↔ snake_case 자동 변환.
- **프록시(proxy)**: JPA가 지연 로딩 등을 위해 엔티티를 상속해 감싸는 대리 객체(그래서 엔티티는 final이면 안 됨).

한 줄 그림: **@Entity + @Id + @GeneratedValue + @Column으로 객체-테이블 매핑을 선언한다. Spring Boot가
필드명을 snake_case 컬럼으로 자동 변환하므로 대부분 @Column(name=...)을 생략해도 된다.**

---

## 1. 학습 내용 — 매핑을 선언하는 법

### @Entity의 두 가지 조건 (왜?)
- **기본 생성자 필수**: JPA는 DB에서 읽은 행으로 객체를 만들 때 **리플렉션으로 매개변수 없는 생성자**를
  호출한다(PART 2.6의 'new 없이 객체 생성', PART 5.3 Reflection과 연결). 그래서 기본 생성자가 있어야 한다
  (외부에서 막 만들지 못하게 `protected`까지 허용).
- **final 금지**: JPA는 지연 로딩 등을 위해 엔티티를 **프록시(상속 기반)로 감싼다**(PART 12 예고). 클래스가
  final이면 상속을 못 해 프록시를 만들 수 없다.

### @Id + @GeneratedValue — PK 생성 전략
| 전략 | 동작 | 적합 DB |
|---|---|---|
| IDENTITY | DB auto_increment | MySQL, H2, PostgreSQL |
| SEQUENCE | DB 시퀀스 객체 | Oracle, PostgreSQL |
| TABLE | 키 생성 전용 테이블 | 모든 DB(느림) |
| AUTO | DB에 맞게 자동 선택 | 기본값 |

- **IDENTITY**: INSERT를 실행해야 그제야 PK를 알 수 있다 → 여러 건을 모아 한 번에 넣는 **배치 INSERT가 불가**.
- **SEQUENCE**: PK를 미리 받아둘 수 있어 빠르고 배치에 유리(Oracle 등).

### @Column 옵션 + 네이밍 전략
```java
@Column(nullable = false, length = 50)
private String productName;   // -> 컬럼 product_name (네이밍 전략이 자동 변환)
@Column(unique = true)
private String code;
```
- `@Column`으로 NOT NULL/길이/UNIQUE 등을 지정. 위반 시 DB 제약으로 저장이 거부된다.
- **네이밍 전략**: Spring Boot는 `SpringPhysicalNamingStrategy`로 camelCase 필드를 snake_case 컬럼으로 자동
  변환한다(`productName` → `product_name`, `stockQuantity` → `stock_quantity`). 그래서 `@Column(name="...")`을
  대부분 생략할 수 있다.

### ★ enum 매핑 — @Enumerated (ORDINAL 함정)
자바 enum을 컬럼에 저장하는 방식은 두 가지다.
```java
@Enumerated(EnumType.STRING)   // "ON_SALE" 같은 '이름'으로 저장
private ProductStatus status;
```
- **STRING**: enum 이름 그대로 저장. 사람이 읽기 쉽고, 상수 순서가 바뀌어도 안전.
- **ORDINAL**: 선언 순서 숫자로 저장(ON_SALE=0, SOLD_OUT=1...). **@Enumerated를 안 붙이면 기본이 ORDINAL이다.**
- ★ **ORDINAL 함정**: 나중에 enum 상수 순서를 바꾸거나 **중간에 새 상수를 끼워 넣으면**, 이미 저장된 숫자의
  '의미'가 통째로 어긋난다(0,1,2가 밀려 기존 데이터가 다른 상태로 해석됨). 되돌리기 어려운 데이터 사고다.
- 그래서 **실무는 거의 항상 STRING**을 쓴다. (실측: status 컬럼에 숫자 1이 아니라 `'SOLD_OUT'` 문자열이 저장됨.)

### 값 타입 — @Embeddable / @Embedded (세분성 미스매치 해결)
11.2에서 본 '세분성(granularity) 미스매치'(객체는 `Address`로 잘게, DB는 컬럼으로 펼침)를 푸는 매핑이다.
```java
@Embeddable                       // 값 타입 클래스
public class Address { private String city; private String zipcode; ... }

@Entity
public class Product {
    @Embedded                     // 엔티티가 값 타입을 품음
    private Address address;
}
```
- `Address`는 **별도 테이블이 아니라** product 테이블의 `city`, `zipcode` 컬럼으로 **펼쳐져** 저장된다.
- 값 타입은 식별자(@Id)가 없고 독립 존재하지 않으며, 생명주기를 품은 엔티티가 따라간다. JPA가 리플렉션으로
  생성하므로 값 타입도 기본 생성자가 필요하다.
- 실측: `select city, zipcode from product`로 컬럼이 펼쳐졌음을 확인하고, 엔티티로 읽으면 다시 `Address`로 묶여 온다.

### @Table — 테이블 단위 옵션 (복합 유니크·인덱스)
`@Column`이 컬럼 단위라면, `@Table`은 테이블 단위 옵션을 건다.
```java
@Table(
    name = "product",
    uniqueConstraints = @UniqueConstraint(name = "uk_name_status", columnNames = {"product_name", "status"}),
    indexes = @Index(name = "idx_code", columnList = "code")
)
```
- **복합 유니크(uniqueConstraints)**: 여러 컬럼을 묶은 유일성. `@Column(unique=true)`는 단일 컬럼만 되므로,
  "이름+상태 조합은 유일"처럼 두 컬럼 조합 제약은 여기서 건다. (실측: 같은 (노트북, ON_SALE)이면 거부,
  같은 노트북이라도 상태가 다르면 허용.)
- **인덱스(indexes)**: 조회 성능용 인덱스를 선언한다(인덱스 자체는 PART 15~16에서 깊이 다룬다).

---

## 2. 실습으로 확인하기

> - **가설 1**: @GeneratedValue(IDENTITY)로 save 후 PK가 자동 채번된다.
> - **가설 2**: productName 필드는 product_name 컬럼으로 매핑된다(네이밍 전략).
> - **가설 3**: @Column(nullable=false) 위반(null)·@Column(unique=true) 위반(중복)은 저장이 거부된다.
> - **가설 4**: @Enumerated(STRING)이면 status 컬럼에 숫자가 아니라 'SOLD_OUT' 문자열이 저장된다.
> - **가설 5**: @Embedded Address는 product 테이블의 city/zipcode 컬럼으로 펼쳐 저장된다.
> - **가설 6**: @Table 복합 유니크 (product_name, status) 조합 중복은 저장이 거부된다.

### 코드 (`com.study.part11_jpa.s04_entity_mapping`)
- `Product` — IDENTITY PK + @Column(nullable/length/unique) + camelCase 필드 + @Enumerated(STRING) status
  + @Embedded Address + @Table(복합 유니크·인덱스).
- `ProductStatus`(enum), `Address`(@Embeddable 값 타입), `ProductRepository`(JpaRepository).
- 테스트 `JpaEntityMappingTest`(@DataJpaTest) — PK 채번/네이밍/NOT NULL/UNIQUE + enum STRING/@Embedded/복합 유니크 검증.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew test --tests "com.study.part11_jpa.s04_entity_mapping.*"
```

### 실행 결과 — 가설과 실제 비교
- `BUILD SUCCESSFUL` — 7개 테스트 모두 통과. ✅
  - identityPk: save 후 id != null(자동 채번).
  - namingStrategy: `select ... where product_name = '노트북'` 네이티브 SQL 성공 → 컬럼이 snake_case(product_name)임을 확인.
  - notNullConstraint / uniqueConstraint: null·중복 code 저장 시 예외 발생(DB 제약 동작).
  - enumStringMapping: status 컬럼 값이 `'SOLD_OUT'`(숫자 1이 아님) → STRING 매핑 확인.
  - embeddedValueType: `select city, zipcode from product`로 펼쳐진 컬럼 확인 + 엔티티로는 Address로 묶여 반환.
  - compositeUniqueConstraint: (노트북, ON_SALE) 중복은 거부, (노트북, SOLD_OUT)은 허용 → 복합 유니크 동작.

---

## 3. 자기 점검

- **Q. @Entity에 기본 생성자가 필수이고 final이 금지인 이유는?**
  - 내 답: JPA가 리플렉션으로 객체를 만들 때 기본 생성자를 호출하므로 필수. 지연 로딩 등을 위해 엔티티를
    프록시(상속)로 감싸야 하므로 final이면 안 된다.

- **Q. IDENTITY와 SEQUENCE의 차이는?**
  - 내 답: IDENTITY는 DB auto_increment라 INSERT 후에야 PK를 알아 배치 INSERT가 불가. SEQUENCE는 시퀀스에서
    PK를 미리 받아둘 수 있어 빠르고 배치에 유리.

- **Q. productName 필드인데 @Column(name="...") 없이 product_name 컬럼이 되는 이유는?**
  - 내 답: Spring Boot의 네이밍 전략(SpringPhysicalNamingStrategy)이 camelCase를 snake_case로 자동 변환하기 때문.

- **Q. @Column(nullable=false)/unique=true 위반은 언제 어떻게 막히나?**
  - 내 답: DB 제약으로 막힌다. 저장(flush) 시 NOT NULL/UNIQUE 위반이면 예외가 나서 커밋되지 않는다.

- **Q. enum은 어떻게 매핑하고, 왜 ORDINAL을 피하나?**
  - 내 답: @Enumerated(EnumType.STRING)으로 이름 문자열 저장. 기본값 ORDINAL은 선언 순서 숫자라, 나중에
    상수 순서를 바꾸거나 중간에 추가하면 기존 데이터의 의미가 어긋난다(데이터 깨짐). 그래서 실무는 STRING.

- **Q. @Embedded(값 타입)는 무엇이고 11.2와 어떻게 연결되나?**
  - 내 답: Address 같은 식별자 없는 값 객체를 엔티티 테이블 컬럼(city/zipcode)으로 펼쳐 매핑한다. 11.2의
    세분성 미스매치(객체는 잘게, DB는 컬럼)를 해결한다. 별도 테이블이 아니라 품은 엔티티의 컬럼이 된다.

- **Q. @Column(unique=true)와 @Table 복합 유니크의 차이는?**
  - 내 답: @Column(unique=true)는 단일 컬럼만. 여러 컬럼 조합의 유일성은 @Table(uniqueConstraints=
    @UniqueConstraint(columnNames={...}))으로 건다. 예: (product_name, status) 조합 유일.
