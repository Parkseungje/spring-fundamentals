# PART 8 — 객체 설계의 진화 → IoC/DI: 8.5 IoC (제어의 역전) + 프레임워크 vs 라이브러리

> 이 문서는 커리큘럼 PART 8의 소단원 중 **8.5 IoC**를 다룬다.
> 8.4에서 남은 숙제("조립은 누가?")를 풀며 IoC(제어의 역전) 개념을 익히고, 프레임워크와 라이브러리의
> 차이를 본다. (아직 Spring 컨테이너는 아니고 '순수 자바 팩토리'로 개념을 먼저 잡는다 — Spring은 8.6)

---

## 0. 들어가기 전에 — 핵심 용어
- **조립(assembly)**: 객체를 생성(new)하고 의존을 주입해 '완성된 객체'로 묶는 일. 8.4까지는 Main이 했다.
- **제어권(control)**: "어떤 구현을 쓸지·언제 객체를 만들지"를 결정하는 권한.
- **IoC(Inversion of Control, 제어의 역전)**: 그 제어권이 '내 코드'에서 '외부(팩토리/프레임워크)'로 넘어가는 것.
- **라이브러리(library)**: 내 코드가 흐름을 쥐고, 필요할 때 호출해 쓰는 도구(내 코드 → 라이브러리).
- **프레임워크(framework)**: 프레임워크가 흐름을 쥐고, 내 코드를 호출하는 것(프레임워크 → 내 코드). = IoC.
- **Hollywood Principle**: "Don't call us, we'll call you"(우리를 부르지 마, 우리가 너를 부를게) — 프레임워크의 동작 방식.
- **IoC 컨테이너**: 객체 생성·연결·생명주기를 대신 관리하는 주체. Spring에선 ApplicationContext(8.6).

한 줄 그림: **8.4까지 Main이 하던 '조립'을 외부(DaoFactory)로 넘기면, "어떤 구현을 쓸지"의 제어권이
Main → 팩토리로 역전된다(IoC). 이 '대신 조립해 주는 주체'를 프레임워크(Spring 컨테이너)로 격상시키는 게 8.6.**

---

## 1. 학습 내용 — 제어권을 넘긴다

### 8.4의 남은 문제: "조립은 누가?"
8.4에서 UserDao는 깨끗해졌지만, 조립(생성+주입)은 여전히 클라이언트가 했다.
```java
// 8.4 Main — Main이 '무엇을 new 할지' 직접 결정한다
ConnectionMaker cm = new NConnectionMaker();   // Main이 구현을 고름
UserDao dao = new UserDao(cm);                 // Main이 조립
```
UserDao는 구현을 모르지만, 결국 **Main이 알고 결정**한다. 제어권이 아직 내 코드에 있다.

### 8.5: 조립을 팩토리로 분리 → 제어권이 넘어간다(IoC)
조립 책임을 `DaoFactory`로 옮긴다. Main은 무엇을 new 할지 모르고, '완성된 객체'를 받기만 한다.
```java
// DaoFactory — 조립(무엇을 만들어 어떻게 연결할지)을 여기에 모음
public UserDao userDao() { return new UserDao(connectionMaker()); }
public ConnectionMaker connectionMaker() { return new NConnectionMaker(); }

// Main — 무엇을 new 할지 모른 채 완성품만 받는다
UserDao dao = new DaoFactory().userDao();
```
→ "어떤 구현을 쓸지"의 결정권이 **Main → DaoFactory(외부)로 역전**됐다. 이것이 **IoC(제어의 역전)**다.
(D로 바꾸려면 DaoFactory의 connectionMaker()만 고치면 되고, Main은 무관.)

### 프레임워크 vs 라이브러리 (호출 방향이 핵심)
| | 라이브러리 | 프레임워크 |
|---|---|---|
| 흐름(제어) 주체 | 내 코드 | 프레임워크 |
| 호출 방향 | 내 코드 → 라이브러리(내가 부른다) | 프레임워크 → 내 코드(프레임워크가 나를 부른다) |
| 예 | 수학 유틸 호출 | Spring이 내 빈을 만들고 호출 |

- **Hollywood Principle**: "Don't call us, we'll call you." 내가 프레임워크를 부르는 게 아니라,
  프레임워크가 내 코드(빈)를 만들고 호출한다. 그래서 **Spring은 라이브러리가 아니라 프레임워크**다.
- IoC 컨테이너: 이렇게 객체의 생성·연결·생명주기를 대신 관리하는 주체. 8.6의 **ApplicationContext**가 그것.

> ★ 8.5의 핵심: 지금 만든 DaoFactory는 '평범한 자바 클래스'다. 개념(제어의 역전)은 이미 일어났다.
> 8.6은 이 팩토리에 `@Configuration`/`@Bean`을 붙여, **조립을 Spring 컨테이너(프레임워크)가 대신 수행**하게
> 만든다 — 같은 IoC를 '내가 만든 팩토리'가 아니라 '프레임워크'가 떠안는 것.

---

## 2. 실습으로 확인하기

> - **가설**: Main이 무엇을 new 할지 모른 채 factory.userDao()로 완성품을 받아 동작한다(제어권이 팩토리로 역전).

### 코드 (`com.study.part08_ioc.s05_ioc`)
- `DaoFactory` — 조립(생성+주입)을 모은 순수 자바 팩토리(8.4의 UserDao/ConnectionMaker 재사용).
- `Main` — factory.userDao()로 완성된 UserDao를 받아 쓴다(직접 new 안 함).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part08_ioc.s05_ioc.Main
```

### 실행 결과 — 가설과 실제 비교
```
[8.5] User{id=s5, name=IoC출발}
=> Main은 무엇을 new 할지 모르고 factory.userDao()로 완성품을 받는다.
   '어떤 구현을 쓸지'의 결정권이 Main -> 팩토리(외부)로 넘어감 = IoC(제어의 역전).
```
- Main은 ConnectionMaker가 N인지 D인지 모른 채 완성된 UserDao를 받아 정상 동작. 제어권 역전 확인. ✅
- 아직 '평범한 자바 팩토리'다 → 8.6에서 Spring 컨테이너가 이 역할을 대신한다.

---

## 3. 자기 점검

- **Q. IoC(제어의 역전)에서 '무엇'의 제어가 '어디서 어디로' 넘어가나?**
  - 내 답: "어떤 구현을 쓸지·객체를 언제 만들지"의 제어권이 내 코드(Main) → 외부(팩토리/컨테이너)로 넘어간다.
    8.4는 Main이 결정, 8.5는 DaoFactory가 결정.

- **Q. 프레임워크와 라이브러리의 결정적 차이는?**
  - 내 답: 호출 방향(제어 주체). 라이브러리는 내 코드가 호출(내 코드→라이브러리), 프레임워크는 프레임워크가
    내 코드를 호출(프레임워크→내 코드). Hollywood Principle. 그래서 Spring은 프레임워크.

- **Q. 8.5의 DaoFactory는 아직 Spring이 아닌데, 그래도 IoC라고 할 수 있나?**
  - 내 답: 그렇다. IoC는 '개념'(제어권이 외부로 넘어감)이고, 그 주체가 내가 만든 팩토리든 Spring 컨테이너든
    상관없다. 8.6은 같은 IoC를 '프레임워크(Spring)'가 떠안게 격상시킬 뿐이다.
