# PART 8 — 객체 설계의 진화 → IoC/DI: 8.4 인터페이스 + 합성 → OCP + 전략 패턴

> 이 문서는 커리큘럼 PART 8의 소단원 중 **8.4 인터페이스 + 합성**을 다룬다.
> 8.3에서 정리한 '상속의 한계 3가지'를, 상속을 버리고 인터페이스 구현 + 합성으로 바꿔 해결한다.
> 여기서 PART 1의 OCP·DIP가 코드로 실현된다. (8.3은 `PART08_8_3_design_pattern.md` 참고)

---

## 0. 들어가기 전에 — 핵심 용어
- **인터페이스(interface)**: 구현 없는 '약속(메서드 시그니처)'의 모음. 클래스는 여러 개를 구현할 수 있다.
- **합성(composition)**: 다른 객체를 '필드로 들고(has-a)' 그 객체에 일을 위임하는 것. 상속(is-a)의 대안.
- **의존(dependency)**: 어떤 객체가 일을 하려고 '필요로 하는' 다른 객체(여기선 UserDao가 필요로 하는 ConnectionMaker).
- **주입(injection)**: 그 의존 객체를 '외부에서 넣어주는' 것. 여기선 생성자로 받는다(생성자 주입).
- **OCP(개방-폐쇄 원칙)**: 확장에는 열리고(새 구현 추가 OK) 수정에는 닫힌다(기존 코드 수정 X).
- **DIP(의존 역전 원칙)**: 구체 클래스가 아니라 추상(인터페이스)에 의존하라.
- **전략 패턴(Strategy)**: 갈아끼울 수 있는 알고리즘(전략)을 인터페이스로 두고 외부에서 주입하는 패턴. OCP의 구현 도구.

한 줄 그림: **UserDao가 ConnectionMaker '인터페이스'에만 의존하고 구현체를 '생성자로 주입'받으면(합성),
새 DB가 생겨도 UserDao는 수정 0(OCP)이고 단일 상속 제약도 사라진다. 단 '조립'은 아직 Main이 한다 → 8.5/8.6.**

---

## 1. 학습 내용 — 상속을 합성으로 바꾸기

### 상속(extends) → 인터페이스 구현 + 합성(has-a)
8.2/8.3은 `UserDao`를 **상속**해 연결 방법을 갈아끼웠다. 8.4는 구조를 뒤집는다.
- '연결 만들기'를 **인터페이스** `ConnectionMaker`로 정의한다(Strategy).
- 그 구현체 `NConnectionMaker`/`DConnectionMaker`는 UserDao와 **상속 관계가 없다**(독립).
- `UserDao`는 그 인터페이스를 **필드로 들고(합성)**, 구현체를 **생성자로 주입**받는다.

```java
public interface ConnectionMaker { Connection makeConnection(); }   // Strategy

public class UserDao {
    private final ConnectionMaker connectionMaker;        // 인터페이스에만 의존(합성)
    public UserDao(ConnectionMaker cm) {                  // 외부에서 주입(생성자 주입)
        this.connectionMaker = cm;
    }
    public void add(User u) { Connection c = connectionMaker.makeConnection(); ... }  // 위임
}
```

### 8.3 한계 3가지가 어떻게 풀리나
| 8.3 한계 | 8.4의 해결 |
|---|---|
| ① 단일 상속 제약 | UserDao가 아무것도 상속 안 함. ConnectionMaker는 인터페이스라 여러 개 구현 가능 |
| ② 컴파일 타임 결합 | "어떤 구현을 쓸지"가 코드에 안 박힘 — 외부가 주입(런타임 결정) |
| ③ 부모-자식 강결합 | 상속이 사라짐. UserDao와 구현체는 인터페이스로만 느슨하게 연결(약결합) |

### SOLID로 보면 — OCP + DIP의 실현
- **OCP**: 새 DB가 생겨도 `ConnectionMaker` 구현체만 추가하면 되고 **UserDao는 수정 0**(확장 O, 수정 X).
- **DIP**: UserDao가 구체 클래스(NConnectionMaker)가 아니라 추상(ConnectionMaker)에 의존한다.
- 전략 패턴 매핑: **Context=UserDao, Strategy=ConnectionMaker, ConcreteStrategy=N/DConnectionMaker.**
  → 전략 패턴은 OCP를 구현하는 도구이고, PART 1의 DIP가 여기서 코드로 실현된다.

### ★ 그래도 남은 한 가지 — "조립은 누가?"
UserDao는 깨끗해졌지만, `new NConnectionMaker()`를 만들어 `new UserDao(cm)`로 넣어주는 **'조립(생성+주입)'
책임은 아직 클라이언트(Main)에 있다.** 이 조립 책임을 외부(컨테이너)로 넘기는 것이 8.5(IoC) / 8.6(ApplicationContext)다.

---

## 2. 실습으로 확인하기

> - **가설**: UserDao는 그대로 두고 주입하는 ConnectionMaker만 N↔D로 바꿔도, UserDao 코드 수정 없이 동작한다(OCP).

### 코드 (`com.study.part08_ioc.s04_strategy`)
- `ConnectionMaker`(인터페이스) + `NConnectionMaker` / `DConnectionMaker`(구현체).
- `UserDao` — 인터페이스에만 의존, 생성자 주입(합성).
- `Main` — 구현체를 주입해 동작 확인.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part08_ioc.s04_strategy.Main
```

### 실행 결과 — 가설과 실제 비교
```
[8.4] User{id=s4, name=전략패턴}
=> UserDao는 ConnectionMaker(인터페이스)에만 의존. 구현 교체에도 UserDao 수정 0(OCP/DIP).
```
- 동작 정상. 구현체를 N→D로 바꾸려면 Main의 `new NConnectionMaker()` 한 줄만 바꾸면 되고, **UserDao는
  손대지 않는다.** ✅ (단 그 '한 줄 조립'을 아직 Main이 함 → 8.5/8.6에서 컨테이너로 이전)

---

## 3. 자기 점검

- **Q. 상속(8.2/8.3) 대신 합성(8.4)으로 바꿔 얻은 것 3가지는?**
  - 내 답: ①단일 상속 제약 해방(UserDao가 상속 안 함) ②컴파일 타임 결합 → 런타임 주입 ③강결합 → 인터페이스로 약결합.

- **Q. 전략 패턴의 Context/Strategy/ConcreteStrategy는 각각 무엇인가?**
  - 내 답: Context=UserDao, Strategy=ConnectionMaker(인터페이스), ConcreteStrategy=N/DConnectionMaker(구현체).

- **Q. 8.4에서 OCP가 만족된다고 보는 근거는?**
  - 내 답: 새 DB(연결 방식)가 생겨도 ConnectionMaker 구현체만 추가하면 되고 UserDao는 수정 0이기 때문(확장 O, 수정 X).

- **Q. 8.4에도 아직 남은 책임은 무엇이고, 어디서 해결되나?**
  - 내 답: `new NConnectionMaker()`를 만들어 UserDao에 주입하는 '조립' 책임이 아직 Main에 있다. 8.5(IoC)/
    8.6(ApplicationContext)에서 이 조립을 Spring 컨테이너로 넘긴다.
