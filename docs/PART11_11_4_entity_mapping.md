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

---

## 2. 실습으로 확인하기

> - **가설 1**: @GeneratedValue(IDENTITY)로 save 후 PK가 자동 채번된다.
> - **가설 2**: productName 필드는 product_name 컬럼으로 매핑된다(네이밍 전략).
> - **가설 3**: @Column(nullable=false) 위반(null)·@Column(unique=true) 위반(중복)은 저장이 거부된다.

### 코드 (`com.study.part11_jpa.s04_entity_mapping`)
- `Product` — IDENTITY PK + @Column(nullable/length/unique) + camelCase 필드(productName/stockQuantity).
- `ProductRepository` — JpaRepository.
- 테스트 `JpaEntityMappingTest`(@DataJpaTest) — PK 채번/네이밍(네이티브 SQL로 product_name 확인)/NOT NULL/UNIQUE 검증.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew test --tests "com.study.part11_jpa.s04_entity_mapping.*"
```

### 실행 결과 — 가설과 실제 비교
- `BUILD SUCCESSFUL` — 4개 테스트 모두 통과. ✅
  - identityPk: save 후 id != null(자동 채번).
  - namingStrategy: `select ... where product_name = '노트북'` 네이티브 SQL 성공 → 컬럼이 snake_case(product_name)임을 확인.
  - notNullConstraint / uniqueConstraint: null·중복 code 저장 시 예외 발생(DB 제약 동작).

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
