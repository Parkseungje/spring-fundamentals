# PART 9 — 테스트와 웹 인프라: 9.1 JUnit 테스트

> 이 문서는 커리큘럼 PART 9의 소단원 중 **9.1 JUnit 테스트**를 다룬다.
> PART 8에서 만든 IoC/DI 코드(`UserDao`, ApplicationContext)를 `main()` 눈 검증 대신 **자동화 단위
> 테스트**로 검증한다.

---

## 0. 들어가기 전에 — 핵심 용어
- **단위 테스트(unit test)**: 코드의 작은 단위(메서드/클래스)가 의도대로 동작하는지 자동으로 확인하는 코드.
- **JUnit**: 자바 표준 테스트 프레임워크. `@Test`가 붙은 메서드를 자동 실행한다(여기선 JUnit 5).
- **단언(assertion)**: "실제 값이 기대 값과 같은가"를 검사하는 문장. 틀리면 테스트 실패.
- **매처(matcher)**: 단언을 자연어처럼 읽히게 해주는 도구. AssertJ `assertThat(x).isEqualTo(y)`, Hamcrest `assertThat(x, is(y))`.
- **픽스처(fixture)**: 테스트에 필요한 공통 준비물(객체·초기 데이터). 매 테스트 전에 깨끗이 세팅한다.
- **`@BeforeEach` / `@BeforeAll`**: 각 테스트 직전마다 실행 / 전체에서 딱 한 번 실행.
- **테스트 독립성(격리)**: 한 테스트가 다른 테스트 결과에 영향을 주지 않는 성질.

한 줄 그림: **테스트는 "사람이 눈으로 확인"을 "코드가 코드를 검증"으로 바꾼다. `@Test`로 검증을 자동화하고,
`@BeforeEach`로 매번 깨끗한 시작 상태(픽스처)를 만들어 테스트들이 서로 독립적으로 돌게 한다.**

---

## 1. 학습 내용 — 왜, 어떻게 테스트하나

### 왜 테스트인가 — main() 검증의 한계
PART 8에서는 `main()`에서 `println`을 찍고 **사람이 눈으로** 결과를 판단했다. 이 방식은:
- 매번 사람이 실행·확인해야 하고(반복·비효율), 출력이 많아지면 놓치기 쉽고, 회귀(전에 되던 게 깨짐)를 못 잡는다.

**단위 테스트**는 이를 "코드가 코드를 검증"으로 바꾼다. 좋은 테스트의 조건: **자동화 · 격리(독립) · 빠름 · 반복 가능.**

### 단언과 매처 — 기대값을 표현하는 두 스타일
```java
// AssertJ (스프링 기본, 메서드 체이닝 — 가독성 좋음)
assertThat(found.getName()).isEqualTo("테스터1");
assertThatThrownBy(() -> dao.get("nope")).isInstanceOf(Exception.class);  // 예외도 검증

// Hamcrest (커리큘럼에서 소개 — 자연어처럼 읽힘)
assertThat(found.getName(), is("테스터1"));
```
둘 다 `spring-boot-starter-test`에 포함돼 있다. 실무는 보통 AssertJ를 주로 쓴다.

### 픽스처와 실행 방식 — @BeforeEach, 그리고 "테스트마다 새 인스턴스"
- **`@BeforeEach`**: 각 `@Test` 실행 '직전'마다 호출된다. 여기서 매번 테이블을 만들고 `deleteAll()`로
  데이터를 비워 **깨끗한 시작 상태**를 만든다 → 테스트들이 서로 영향을 안 준다(독립성/격리).
- **`@BeforeAll`** 과 차이: BeforeAll은 전체에서 한 번만(무거운 1회성 준비), BeforeEach는 매 테스트마다.
- **테스트마다 새 인스턴스**: JUnit은 `@Test` 메서드마다 테스트 클래스의 인스턴스를 새로 만든다 →
  필드에 남은 상태가 다음 테스트로 새지 않게 해 독립성을 더한다.
- **통합 테스트 DB 잔여 데이터**: 실제 DB를 쓰는 통합 테스트에선 `@Transactional`(테스트 후 롤백)이나
  `@Sql`(초기화 스크립트)로 격리한다. (PART 9 이후 통합 테스트에서 다시.)

> ★ 핵심: 테스트가 "어쩌다 통과/실패"하면 안 된다. **@BeforeEach로 매번 같은 시작 상태**를 보장해야
> 결과가 '반복 가능'해진다. (PART 7의 "어쩌다 되는 코드는 위험"과 같은 사고.)

### IoC/DI도 테스트로 검증
8.6에서 `println`으로 보던 "DI 완성 / 싱글톤(dao1 == dao2)"도 테스트로 자동 검증한다.
```java
var ctx = new AnnotationConfigApplicationContext(DaoFactory.class);
assertThat(ctx.getBean(UserDao.class)).isNotNull();                  // DI로 완성된 빈
assertThat(ctx.getBean(UserDao.class)).isSameAs(ctx.getBean(UserDao.class));  // 싱글톤
```

---

## 2. 실습으로 확인하기

> - **가설 1**: UserDao add/get/count가 의도대로 동작하고, @BeforeEach 덕에 각 테스트가 독립적으로 통과한다.
> - **가설 2**: 컨테이너가 DI로 완성한 UserDao 빈은 not null이고, 여러 번 꺼내도 같은 인스턴스다(싱글톤).

### 코드 (`src/test/java/com/study/part09_test/s01_junit`)
- `UserDaoTest` — add/get(매처 두 종류), count(상태 검증), 독립성, 예외 검증 + @BeforeEach 픽스처(createTable/deleteAll).
- `DiContextTest` — ApplicationContext로 빈 주입(DI)·싱글톤(isSameAs) 검증.
- (테스트 대상은 PART 8.4의 `UserDao`. 픽스처/검증을 위해 `deleteAll()`·`getCount()`를 그 DAO에 추가했다.)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew test --tests "com.study.part09_test.s01_junit.*"
```
결과 리포트는 `build/reports/tests/test/index.html`에서 볼 수 있다.

### 실행 결과 — 가설과 실제 비교
- `BUILD SUCCESSFUL` + 6개 테스트 모두 통과. ✅
  - `UserDaoTest`: addAndGet / count / 독립성 / 예외 검증 — 매 테스트가 @BeforeEach로 0에서 시작해 독립적으로 통과.
  - `DiContextTest`: 빈 not null(DI) / `isSameAs`(싱글톤) 통과 → 8.6의 IoC/DI가 테스트로 재확인됨.

---

## 3. 자기 점검

- **Q. main() 출력 확인 대신 단위 테스트를 쓰는 이유는?**
  - 내 답: 사람이 매번 눈으로 확인하는 건 반복·비효율이고 회귀를 못 잡는다. 테스트는 코드가 코드를
    자동·반복 검증하므로 빠르고 신뢰할 수 있다. (자동화·격리·빠름·반복 가능)

- **Q. @BeforeEach로 deleteAll을 하는 이유는?**
  - 내 답: 각 테스트가 깨끗한 시작 상태에서 출발해 서로 영향을 주지 않게(독립성/격리) 하기 위해. 안 하면
    이전 테스트가 남긴 데이터로 결과가 '어쩌다' 바뀐다. (UserDaoTest의 independentFromOtherTests)

- **Q. @BeforeEach와 @BeforeAll의 차이는?**
  - 내 답: BeforeEach는 각 테스트 직전마다, BeforeAll은 전체에서 한 번만 실행된다. 매번 초기화가 필요하면
    BeforeEach, 무거운 1회성 준비면 BeforeAll.

- **Q. IoC/DI(싱글톤)를 테스트로 어떻게 검증했나?**
  - 내 답: ApplicationContext에서 getBean으로 두 번 꺼내 `isSameAs`로 동일 인스턴스인지 단언(싱글톤),
    빈이 not null인지로 DI 완성을 확인. (DiContextTest)
