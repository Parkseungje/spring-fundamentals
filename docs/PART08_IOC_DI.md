# PART 8 — 객체 설계의 진화 → IoC/DI (스프링 진입)

> 이 문서는 커리큘럼 PART 8(8.1~8.6)을 다룬다. "DAO 코드 한 줄이 어떻게 Spring의 IoC/DI까지 진화하는지"를
> **하나의 UserDao를 5단계로 리팩토링하는 여정**으로 본다. 여기서부터 PART 1의 SOLID가 실제로 살아 움직인다.

---

## 0. 들어가기 전에 — 핵심 용어
- **DAO(Data Access Object)**: DB 접근(저장·조회)을 담당하는 객체. 여기선 `UserDao`.
- **관심사의 분리(SoC)**: 성격이 다른 일(연결 만들기 vs 쿼리 실행)을 떼어내 따로 관리하는 것(= SRP).
- **템플릿 메소드 패턴**: 변하지 않는 '흐름'은 부모가, 변하는 '한 부분'만 자식이 구현. (Spring JdbcTemplate의 어원)
- **전략 패턴(Strategy)**: 갈아끼울 수 있는 알고리즘(전략)을 인터페이스로 두고 외부에서 주입. OCP의 구현 도구.
- **합성(composition)**: 다른 객체를 '필드로 들고(가지고)' 위임하는 것. 상속(extends)의 대안.
- **IoC(제어의 역전)**: 객체 생성·연결의 '제어권'이 내 코드에서 외부(프레임워크/컨테이너)로 넘어가는 것.
- **DI(의존관계 주입)**: 어떤 객체가 필요로 하는 다른 객체(의존)를 외부가 런타임에 넣어주는 것. IoC의 한 형태.
- **빈(Bean)**: Spring 컨테이너가 만들고 관리하는 객체.
- **ApplicationContext**: Spring의 IoC 컨테이너. 빈을 만들고·연결하고·생명주기를 관리한다.

한 줄 그림: **전통 DAO(모든 책임 한 곳) → 관심사 분리 → 템플릿 메소드 → 전략 패턴(인터페이스+합성) →
IoC 컨테이너(객체 생성·주입을 Spring이 대신)**. 이 진화가 곧 SOLID(SRP·OCP·DIP)의 실현이다.

---

## 1. 학습 내용 — 5단계 리팩토링 여정

각 단계는 `com.study.part08_ioc.stageN_xxx` 패키지의 실행 가능한 코드로 있다. 같은 UserDao가 단계마다
어떻게 바뀌는지 비교하며 읽는 것이 핵심이다.

### stage1 — 전통 DAO: 모든 책임을 한 메서드가 떠안음 (8.1)
`add()`/`get()` 한 메서드 안에 ① 드라이버 로딩·접속 정보 ② SQL ③ 바인딩 ④ 실행 ⑤ 자원 해제 ⑥ 예외 처리가
전부 뒤엉켜 있다. 게다가 '드라이버 로딩+접속' 코드가 add/get에 **그대로 복붙**돼 있다.
- 문제: DB가 바뀌면 **모든 메서드를 고쳐야** 한다(변경 1번 = 수정 N곳). SRP·OCP·DIP 위반.

### stage2 — 관심사의 분리 1단계: 메서드 추출 (8.2)
중복되던 접속 코드를 `private getConnection()` 한 곳으로 모은다("같은 관심끼리 모은다" = SRP).
- 좋아진 점: 중복 제거 + DB 정보가 바뀌어도 **getConnection 한 곳만** 고치면 된다.
- 남은 한계: getConnection이 여전히 UserDao 안에 있어, "UserDao를 안 고치고 연결 방식만 교체"는 불가.

### stage3 — 관심사의 분리 2단계: 추상클래스 + 템플릿 메소드 (8.2~8.3)
요구 진화: "고객사마다 DB가 다른데 UserDao 핵심 코드는 안 건드리고 싶다." → `getConnection()`을 **추상
메서드**로 만든다. 변하지 않는 흐름(add/get)은 부모(`UserDao`), 변하는 연결 방법은 자식(`NUserDao`/`DUserDao`).
- 두 패턴 동시 적용: **템플릿 메소드 패턴**(부모=흐름, 자식=변하는 부분) + **팩토리 메소드 패턴**(객체 생성을 자식에 위임).
- 상속의 한계(→ stage4): 단일 상속 제약 / "어떤 자식을 쓸지"가 `new NUserDao()`로 코드에 박힘(컴파일 타임 결합) / 부모 변경이 자식에 파급.

### stage4 — 인터페이스 + 합성 → OCP + 전략 패턴 (8.4)
상속 대신 **인터페이스(`ConnectionMaker`) + 합성**으로 전환. UserDao는 인터페이스에만 의존하고, 구현체를
**생성자로 주입**받아 필드로 들고 위임한다.
- 좋아진 점: 새 DB가 생겨도 구현체만 추가, **UserDao는 수정 0**(OCP). 단일 상속 제약 해방. 결합이 런타임으로.
- 전략 패턴 매핑: Context=UserDao, Strategy=ConnectionMaker, ConcreteStrategy=N/DConnectionMaker. → 여기서 PART 1의 **DIP**가 실현된다.
- 남은 한 가지: 그럼 `new NConnectionMaker()`를 만들어 UserDao에 넣는 '조립'은 **누가** 하나? 아직 Main이 한다 → stage5.

### stage5 — IoC 컨테이너: ApplicationContext + DI + 싱글톤 (8.5~8.6)
stage4에서 Main이 하던 '생성 + 주입(조립)'을 `DaoFactory`(`@Configuration`/`@Bean`)에 적어두고, **Spring
컨테이너(ApplicationContext)가 대신** 실행한다.
- **IoC(제어의 역전)**: "어떤 구현을 쓸지, 객체를 언제 만들지"의 제어권이 내 코드 → 컨테이너로 넘어감.
  Hollywood Principle("Don't call us, we'll call you") — 그래서 Spring은 라이브러리가 아니라 **프레임워크**.
- **DI**: `DaoFactory.userDao()`가 `connectionMaker()`를 UserDao 생성자에 넣어줌 = 의존관계를 컨테이너가 연결.
- **싱글톤 레지스트리**: `getBean("userDao")`를 여러 번 호출해도 **객체는 한 개**(같은 인스턴스 반환).
  주의: 싱글톤 빈은 **stateless**여야 안전 — 인스턴스 변수에 요청별 데이터를 보관하면 동시성 버그(PART 7)!

| 단계 | 핵심 변화 | 해결한 것 | 남은 한계 |
|---|---|---|---|
| stage1 전통 | 모든 책임 한 곳 | (동작은 함) | 중복·변경 N곳 |
| stage2 메서드 추출 | getConnection 분리 | 중복 제거, 변경 한 곳 | UserDao 안에 갇힘 |
| stage3 템플릿 메소드 | getConnection 추상화 | 흐름/연결 분리 | 상속 제약, 컴파일 결합 |
| stage4 전략 패턴 | 인터페이스+합성+주입 | OCP/DIP, UserDao 수정 0 | 조립을 Main이 함 |
| stage5 IoC | 컨테이너가 조립 | 제어권 이전, 싱글톤·DI | (Spring 본격 시작점) |

---

## 2. 실습으로 확인하기

> - **가설 1**: stage1→stage5로 갈수록 'DB 교체 시 UserDao 수정량'이 줄어 stage4부터는 0이 된다.
> - **가설 2**: stage5에서 getBean으로 꺼낸 UserDao는 동작하고(DI 완성), 여러 번 꺼내도 같은 인스턴스다(싱글톤).

### 실행
아래 명령은 모두 **프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
`runStage` 태스크에 `-Pmain=`으로 단계별 Main을 지정한다.

```bash
./gradlew runStage -Pmain=com.study.part08_ioc.stage1_traditional.Main
./gradlew runStage -Pmain=com.study.part08_ioc.stage2_method_extract.Main
./gradlew runStage -Pmain=com.study.part08_ioc.stage3_template_method.Main
./gradlew runStage -Pmain=com.study.part08_ioc.stage4_strategy.Main
./gradlew runStage -Pmain=com.study.part08_ioc.stage5_ioc_container.Main
```

### 실행 결과 — 가설과 실제 비교
- stage1~4: `add` 후 `get`이 정상 동작(`User{id=..., name=...}` 출력). 단계가 올라갈수록 UserDao가
  연결 방식에서 분리된다(코드를 직접 비교해볼 것).
- **stage5**: `getBean("userDao")`로 꺼낸 UserDao가 동작하고, `dao1 == dao2`가 **true**로 출력 → 싱글톤
  레지스트리 확인. 객체 생성·주입을 컨테이너가 대신했다(IoC/DI). ✅

---

## 3. 자기 점검

- **Q. IoC(제어의 역전)란 무엇이고, 무엇의 '제어'가 '역전'되나?**
  - 내 답: "어떤 구현을 쓸지·객체를 언제 만들지"의 제어권이 내 코드에서 외부(컨테이너)로 넘어가는 것.
    stage4까지는 Main이 new+주입했지만 stage5는 컨테이너가 한다. (stage4 vs stage5)

- **Q. DI는 IoC와 무슨 관계인가?**
  - 내 답: DI(의존관계 주입)는 IoC를 구현하는 한 형태. 객체가 필요로 하는 의존을 외부가 런타임에 넣어준다.
    DaoFactory가 ConnectionMaker를 UserDao에 주입하는 것이 DI. (stage5 DaoFactory)

- **Q. 생성자 주입이 권장되는 이유는?**
  - 내 답: 필드를 final로 만들 수 있어 불변·필수 의존 보장, 순환 참조를 생성 시점에 감지, 테스트 시 가짜
    객체를 넣기 쉬움. (stage4/5의 `UserDao(ConnectionMaker)`)

- **Q. 싱글톤 빈에서 인스턴스 변수에 요청별 데이터를 저장하면 왜 위험한가?**
  - 내 답: 모든 요청이 같은 빈 인스턴스를 공유하므로(싱글톤), 인스턴스 변수에 요청별 상태를 두면 스레드끼리
    값이 덮어써진다(PART 7의 공유 변수 동시성 문제). 그래서 싱글톤 빈은 stateless로 둔다. (8.6)

- **Q. 전략 패턴 / 템플릿 메소드 패턴은 각각 stage 몇에서 쓰였고 무엇이 다른가?**
  - 내 답: 템플릿 메소드=stage3(상속, 부모 흐름+자식이 변하는 부분 구현), 전략 패턴=stage4(합성, 인터페이스
    구현체를 외부 주입). 상속이냐 합성이냐가 핵심 차이. (stage3 vs stage4)
