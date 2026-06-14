# PART 8 — 객체 설계의 진화 → IoC/DI: 8.6 ApplicationContext + DI + 싱글톤 레지스트리

> 이 문서는 커리큘럼 PART 8의 마지막 소단원 **8.6 ApplicationContext + DI + 싱글톤**을 다룬다.
> 8.5의 '평범한 자바 팩토리'에 Spring 어노테이션을 붙여 진짜 IoC 컨테이너로 만든다. 여기서 Spring이
> 본격적으로 등장한다. (8.5는 `PART08_8_5_ioc.md` 참고)

---

## 0. 들어가기 전에 — 핵심 용어
- **빈(Bean)**: Spring 컨테이너가 만들고 관리하는 객체.
- **ApplicationContext**: Spring의 IoC 컨테이너. 빈을 생성·연결(주입)·관리한다. (BeanFactory의 확장판, 실무 표준)
- **`@Configuration`**: "이 클래스는 빈 설정(설계도)"이라고 Spring에 알리는 어노테이션.
- **`@Bean`**: "이 메서드가 반환하는 객체를 빈으로 등록하라". 메서드 이름이 빈 이름이 된다.
- **`getBean(이름, 타입)`**: 컨테이너에서 빈을 꺼내는 메서드.
- **DI(의존관계 주입)**: 어떤 빈이 필요로 하는 다른 빈(의존)을 컨테이너가 런타임에 넣어주는 것.
- **싱글톤 레지스트리**: 컨테이너가 각 빈을 '딱 하나만' 만들어 재사용하는 것(getBean 여러 번 → 같은 인스턴스).
- **stateless**: 인스턴스 변수에 (요청별) 상태를 두지 않는 것. 싱글톤 빈의 안전 조건.

한 줄 그림: **8.5 팩토리에 @Configuration/@Bean을 붙이면, ApplicationContext(컨테이너)가 그 설계도를
읽어 빈을 만들고 의존을 주입(DI)한다. 빈은 기본 싱글톤이라 몇 번 꺼내도 같은 객체다(단 stateless여야 안전).**

---

## 1. 학습 내용 — 컨테이너가 조립을 떠안다

### 평범한 팩토리 → 컨테이너 설계도 (어노테이션 두 개)
8.5의 `DaoFactory`는 평범한 자바 클래스였다. 8.6은 거기에 두 어노테이션만 붙인다.
```java
@Configuration                       // "이 클래스는 빈 설계도"
public class DaoFactory {
    @Bean public UserDao userDao() { return new UserDao(connectionMaker()); }  // 빈 등록 + DI
    @Bean public ConnectionMaker connectionMaker() { return new NConnectionMaker(); }
}
```
이제 조립을 **내가 호출하지 않는다.** ApplicationContext가 이 설계도를 읽어 빈을 만들고 연결한다.
```java
ApplicationContext ctx = new AnnotationConfigApplicationContext(DaoFactory.class);
UserDao dao = ctx.getBean("userDao", UserDao.class);   // 완성된 빈을 받기만
```
→ 8.5의 IoC(제어의 역전)를 이제 **프레임워크(Spring 컨테이너)가 떠안았다.**

### DI(의존관계 주입)가 일어나는 지점
`userDao()` 안에서 `connectionMaker()`를 호출해 UserDao 생성자에 넣는다. 즉 "UserDao는 ConnectionMaker가
필요하다"는 **의존관계**를 컨테이너가 런타임에 **연결(주입)**해 준다. 이것이 DI이고, DI는 IoC의 한 형태다.
- 주입 방식 3가지: **생성자 주입(권장)**, Setter 주입, 필드 주입.
- 생성자 주입 권장 이유: 필드를 final로 만들어 불변·필수 의존 보장 / 순환 참조를 생성 시점에 감지 / 테스트 시 가짜 객체 주입이 쉬움.

### 싱글톤 레지스트리 — 빈은 기본 한 개
`@Bean` 메서드가 코드상 여러 번 불릴 것 같지만, 컨테이너는 각 빈을 **딱 하나만** 만들어 재사용한다.
그래서 `getBean("userDao")`를 100번 호출해도 **항상 같은 인스턴스**다.
- 왜 싱글톤? 빈은 보통 stateless 서비스라, 요청마다 새로 만들 필요 없이 하나를 공유하는 게 효율적이다.
- ★ **주의(PART 7 직결)**: 싱글톤 빈은 모든 요청/스레드가 공유하므로, **인스턴스 변수에 요청별 데이터를
  저장하면** 스레드끼리 값이 덮어써지는 동시성 버그가 난다(7.1/7.4의 공유 변수 문제). → 싱글톤 빈은 **stateless**로.

### BeanFactory vs ApplicationContext
- **BeanFactory**: IoC 컨테이너의 기본 인터페이스(빈 생성·DI).
- **ApplicationContext**: BeanFactory + 부가 기능(국제화 메시지, 이벤트 발행, 환경 추상화 등). 실무 표준.

---

## 2. 실습으로 확인하기

> - **가설 1**: getBean으로 꺼낸 UserDao가 동작한다(컨테이너가 의존성까지 주입해 완성 = DI).
> - **가설 2**: getBean을 여러 번 호출해도 같은 인스턴스다(싱글톤 레지스트리) → `dao1 == dao2`가 true.

### 코드 (`com.study.part08_ioc.s06_appcontext_di`)
- `DaoFactory` — `@Configuration`/`@Bean`을 붙인 설계도(8.4의 UserDao/ConnectionMaker 재사용).
- `Main` — `AnnotationConfigApplicationContext`로 컨테이너를 띄우고 getBean으로 빈을 꺼내 동작·싱글톤 확인.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part08_ioc.s06_appcontext_di.Main
```

### 실행 결과 — 가설과 실제 비교
```
[8.6] User{id=s6, name=ApplicationContext}
[8.6] dao1 == dao2 ? true  (true = 싱글톤 레지스트리)
=> 객체 생성·주입(조립)을 컨테이너가 대신한다(IoC/DI). getBean은 항상 같은 싱글톤 빈을 준다.
```
- 가설 1: getBean으로 받은 UserDao가 add/get 정상 동작 → DI로 의존(ConnectionMaker)까지 주입돼 완성됨. ✅
- 가설 2: `dao1 == dao2`가 **true** → 싱글톤 레지스트리 확인. ✅

---

## 3. PART 8 전체 정리 — 리팩토링 여정 한눈에
| 단계 | 핵심 변화 | 해결 | 남은 한계 |
|---|---|---|---|
| 8.1 전통 DAO | 모든 책임 한 곳 | (동작만) | 중복·변경 N곳 |
| 8.2 관심사 분리 | 메서드 추출 → 추상클래스 | 중복 제거, 부모 수정 0 | 상속 제약, 컴파일 결합 |
| 8.3 패턴과 한계 | 템플릿/팩토리 메소드 인식 | (분석) | 상속의 3한계 |
| 8.4 전략 패턴 | 인터페이스+합성+주입 | OCP/DIP, UserDao 수정 0 | 조립을 Main이 함 |
| 8.5 IoC | 팩토리가 조립 | 제어권 역전 | 아직 평범한 자바 |
| 8.6 ApplicationContext | 컨테이너가 조립 | IoC/DI·싱글톤(Spring) | (PART 9~로 계속) |

---

## 4. 자기 점검

- **Q. @Configuration / @Bean은 각각 무슨 역할인가?**
  - 내 답: @Configuration은 "이 클래스가 빈 설계도"임을 알리고, @Bean은 "이 메서드 반환 객체를 빈으로
    등록"한다(메서드 이름이 빈 이름). 컨테이너가 이를 읽어 빈을 생성·주입한다.

- **Q. DI는 8.6 코드 어디서 일어나나?**
  - 내 답: DaoFactory.userDao()가 connectionMaker()를 UserDao 생성자에 넣는 부분. "UserDao는
    ConnectionMaker가 필요하다"는 의존을 컨테이너가 연결(주입)한다.

- **Q. getBean을 여러 번 호출해도 같은 객체인 이유는?**
  - 내 답: 컨테이너가 빈을 하나만 만들어 재사용하기 때문(싱글톤 레지스트리). 그래서 dao1 == dao2가 true.

- **Q. 싱글톤 빈에 인스턴스 변수로 요청별 상태를 저장하면 왜 위험한가?**
  - 내 답: 모든 스레드가 같은 빈 인스턴스를 공유하므로, 인스턴스 변수에 요청별 상태를 두면 값이 서로
    덮어써진다(PART 7 공유 변수 동시성 버그). 그래서 싱글톤 빈은 stateless로 둬야 한다.

- **Q. BeanFactory와 ApplicationContext의 관계는?**
  - 내 답: ApplicationContext는 BeanFactory(빈 생성·DI)에 국제화·이벤트·환경 추상화 등을 더한 확장판이며
    실무 표준이다.
