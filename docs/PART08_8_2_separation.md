# PART 8 — 객체 설계의 진화 → IoC/DI: 8.2 관심사의 분리

> 이 문서는 커리큘럼 PART 8의 소단원 중 **8.2 관심사의 분리**를 다룬다.
> 8.1의 고통(연결 코드 중복 → DB 변경 시 모든 메서드 수정)을 두 단계로 줄인다: 1단계 메서드 추출,
> 2단계 추상클래스(템플릿 메소드). (8.1은 `PART08_8_1_traditional_dao.md` 참고)

---

## 0. 들어가기 전에 — 핵심 용어
- **관심사의 분리(SoC, Separation of Concerns)**: 성격이 다른 일을 떼어내 따로 관리하는 것. "관심이
  같은 것끼리 모으고 다른 것은 떨어뜨려라" = SRP의 실천.
- **메서드 추출(extract method)**: 중복되거나 한 덩어리인 코드를 별도 메서드로 빼내는 리팩토링.
- **추상 메서드 / 추상클래스**: 몸통 없이 선언만 있는 메서드 / 그런 메서드를 가진 클래스(직접 `new` 불가, 자식이 구현).
- **템플릿 메소드 패턴**: 변하지 않는 '흐름(뼈대)'은 부모가 정하고, 변하는 '한 부분'만 자식이 구현하는 패턴.
- **컴파일 타임 결합**: "어떤 구현을 쓸지"가 `new NUserDao()`처럼 소스 코드에 박혀, 바꾸려면 코드를 고쳐야 하는 상태.

한 줄 그림: **8.2는 8.1의 중복 연결 코드를 ① 한 메서드로 추출하고(수정 N곳 → 1곳), ② 그 메서드를
추상화해 자식이 갈아끼우게(연결 방법 교체 가능) 만든다. 단 "어떤 자식을 쓸지"는 아직 코드에 박힌다.**

---

## 1. 학습 내용 — 두 단계로 분리하기

### 1단계 — 메서드 추출 (중복 제거)
8.1에서 add/get에 복붙돼 있던 '드라이버 로딩 + 접속' 코드를 `private getConnection()` 한 곳으로 모은다.
```java
public void add(User u) { Connection c = getConnection(); ... }   // 중복 사라짐
public User get(String id) { Connection c = getConnection(); ... }
private Connection getConnection() { ... }                         // 연결 관심사 한 곳에
```
- 성과: 중복 제거 + **DB 접속 정보가 바뀌어도 getConnection 한 곳만** 고치면 된다(8.1의 '수정 N곳' → '수정 1곳').
- 남은 한계: getConnection이 **여전히 UserDao 안에** 있다. "UserDao 코드는 안 건드리고 연결 방식만 교체"는 불가.

### 2단계 — 추상클래스로 확장 (템플릿 메소드 패턴)
요구 진화: "고객사마다 DB가 다른데, UserDao의 핵심 흐름(add/get)은 수정·공개하지 않고 '연결 방법'만
갈아끼우고 싶다." → `getConnection()`을 **추상 메서드**로 만든다.
```java
public abstract class AbstractUserDao {
    public void add(User u) { Connection c = getConnection(); ... }   // 변하지 않는 흐름(부모)
    protected abstract Connection getConnection();                    // 변하는 부분(자식이 구현)
}
class NUserDao extends AbstractUserDao { Connection getConnection(){ ...N방식... } }
class DUserDao extends AbstractUserDao { Connection getConnection(){ ...D방식... } }
```
- **템플릿 메소드 패턴**: 부모가 '흐름(뼈대)'을 고정하고, 변하는 한 부분(연결)만 자식이 채운다.
- 성과: 새 고객사(연결 방식)가 생겨도 **부모 코드는 한 줄도 안 바뀌고** 자식 클래스만 추가.
- 남은 한계: "어떤 자식을 쓸지"가 `new NUserDao()`처럼 **코드에 박힌다(컴파일 타임 결합)**. 또 자바는
  단일 상속이라 UserDao를 상속하면 다른 부모를 못 갖는다. → 8.3에서 이 한계를 분석하고 8.4에서 해결.

> ★ 8.2의 핵심 진전: "DB 바뀌면 몇 곳 고치나?"가 8.1의 'N곳' → 1단계 '1곳' → 2단계 '부모 0곳(자식만 추가)'.
> 다만 "교체 결정"이 아직 코드(컴파일 타임)에 묶여 있다는 점이 다음 단계로의 숙제다.

---

## 2. 실습으로 확인하기

> - **가설 1**: 1단계 — 연결 코드가 getConnection 한 곳으로 모여, 동작은 같고 중복은 사라진다.
> - **가설 2**: 2단계 — 부모(AbstractUserDao)를 안 고치고도 N/DUserDao가 각자 다른 연결로 동작한다.

### 코드 (`com.study.part08_ioc.s02_separation`)
- `UserDaoMethodExtract` — 1단계: private getConnection으로 중복 제거.
- `AbstractUserDao`(추상) + `NUserDao` / `DUserDao` — 2단계: 추상 메서드 + 자식 구현(템플릿 메소드).
- `Main` — 1단계·2단계를 차례로 실행해 비교.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part08_ioc.s02_separation.Main
```

### 실행 결과 — 가설과 실제 비교
```
== 8.2 - 1단계: 메서드 추출 ==
[1단계] User{id=u2, name=김유신}              ← 동작 동일, 연결 코드는 한 곳으로
== 8.2 - 2단계: 추상클래스(템플릿 메소드) ==
[2단계 N] User{id=n1, name=N-홍길동}          ← 부모 흐름 그대로, N 자식의 연결로 동작
[2단계 D] User{id=d1, name=D-임꺽정}          ← D 자식의 연결로 동작 (부모 수정 0)
```
- 1단계: 중복이 사라져도 기능은 그대로. ✅
- 2단계: 부모(AbstractUserDao)를 안 고치고 자식만 바꿔 다른 연결로 동작. ✅ 단 어떤 자식을 쓸지는
  `new NUserDao()`로 코드에 박혀 있음(다음 단계 숙제).

---

## 3. 자기 점검

- **Q. 1단계 메서드 추출로 무엇이 줄었나?**
  - 내 답: 중복된 연결 코드가 getConnection 한 곳으로 모여, DB 정보 변경 시 고칠 곳이 'N곳 → 1곳'으로 줄었다.

- **Q. 2단계에서 getConnection을 '추상 메서드'로 만든 이유는?**
  - 내 답: 변하지 않는 흐름(add/get)은 부모에 두고, 변하는 연결 방법만 자식이 구현하게 해서, 부모 코드를
    안 고치고 연결 방식을 교체할 수 있게 하려고(템플릿 메소드 패턴).

- **Q. 2단계(상속) 구조에 아직 남은 한계 두 가지는?**
  - 내 답: ① "어떤 자식을 쓸지"가 `new NUserDao()`로 코드에 박힘(컴파일 타임 결합). ② 자바 단일 상속이라
    UserDao를 상속하면 다른 부모를 못 가짐. → 8.4에서 인터페이스+합성으로 해결.
