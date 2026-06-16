# PART 12 — 프록시의 진화와 Spring AOP: 12.6 동적 프록시 — Reflection → JDK 동적 프록시 → CGLIB

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.6 동적 프록시**를 다룬다.
> 12.4에서 프록시를 손으로 만들었다. 그러나 적용 대상이 100개면 거의 같은 프록시 클래스 100개를 써야 한다
> (유지보수 지옥). 이 반복을 '런타임 자동 생성'으로 푸는 것이 동적 프록시다. 그 기반(Reflection)부터 두
> 구현(JDK 동적 프록시, CGLIB)까지 단계적으로 본다. PART 12의 기술적 핵심 단원이다.

---

## 0. 들어가기 전에 — 핵심 용어
- **Reflection(리플렉션)**: 런타임에 클래스·메서드 정보를 들여다보고(introspection), 이름만으로 메서드를 동적 호출하는 기능.
- **method.invoke(target, args)**: 리플렉션으로 'target의 그 메서드'를 실제로 실행하는 호출.
- **동적 프록시(dynamic proxy)**: 프록시 클래스를 소스에 미리 안 쓰고, 런타임에 자동 생성하는 프록시.
- **JDK 동적 프록시**: 자바 표준이 제공. '인터페이스'를 구현한 프록시를 런타임 생성. 핸들러 = `InvocationHandler`.
- **CGLIB**: 바이트코드 조작 라이브러리. 대상 '클래스를 상속'한 프록시를 생성. 핸들러 = `MethodInterceptor`.
- **InvocationHandler / MethodInterceptor**: 프록시로 들어온 모든 호출이 거쳐 가는 '한 곳'(부가 기능을 여기서 처리).
- **바이트코드(bytecode)**: 자바 소스가 컴파일된 중간 코드(JVM이 실행). CGLIB는 이걸 런타임에 생성·조작한다.

한 줄 그림: **수동 프록시는 대상 수만큼 클래스가 늘어난다. 동적 프록시는 호출을 한 곳(핸들러)으로 모으고
프록시 클래스를 런타임에 자동 생성해, '핸들러 1개로 대상 N개'를 처리한다. 인터페이스가 있으면 JDK 동적
프록시, 없으면 CGLIB(클래스 상속, 단 final 불가).**

---

## 1. 학습 내용

### 1-0. 로우레벨의 고통 — 수동 프록시의 클래스 폭발
12.4의 프록시는 인터페이스의 모든 메서드를 손으로 구현해 위임해야 했다. 그래서:
- 인터페이스에 메서드가 10개면 위임 코드 10번.
- 프록시를 붙일 대상(서비스)이 100개면 거의 똑같은 프록시 클래스 100개.
- 로그 형식 하나만 바꿔도 100개 클래스를 다 수정.

→ "변하지 않는 부가 코드(로그)를 한 곳에 모으고 싶다"는 12.3의 동기가 프록시에서도 그대로 남았다. 이걸
런타임 자동 생성으로 푸는 게 동적 프록시다.

### 1-1. Reflection — 동적 프록시의 출발점
Reflection은 런타임에 클래스/메서드를 들여다보고, **메서드 이름만 알면** 어떤 메서드든 호출한다.
```java
Method method = target.getClass().getMethod("order", String.class);
Object result = method.invoke(target, "노트북");   // 동적 호출
```
- 이점: 대상·메서드가 무엇이든 **하나의 일반 코드**로 가로채 부가 기능을 끼울 수 있다(메서드별 위임 코드 불필요).
- **한계**: 메서드를 '문자열 이름'으로 다뤄 **컴파일 시점 오류 검출이 안 된다.** `"oder"`(오타)도 컴파일은
  통과하고 실행 중에야 `NoSuchMethodException`이 터진다. → 그래서 Reflection은 일반 비즈니스 코드가 아니라
  **프레임워크 개발용**이다(스프링 내부가 이걸 쓴다). 동적 프록시 두 구현도 내부에서 리플렉션을 쓴다.

> ★ "컴파일 시점 검출이 안 된다"가 왜 위험한가 — 보통 메서드 호출 `target.order(...)`는 오타를 컴파일러가
> 즉시 잡는다. 리플렉션은 이름이 문자열이라 컴파일러가 못 본다. 안전망이 사라지는 대신 극한의 유연성을
> 얻는 맞교환이다(그래서 '프레임워크가 사용자 코드를 다룰 때'처럼 대상이 미정인 상황에 쓴다).

### 1-2. JDK 동적 프록시 — 인터페이스 기반, 핸들러 1개로 자동 생성
자바 표준이 제공한다. `InvocationHandler` 하나만 만들면, `Proxy.newProxyInstance(...)`가 그 인터페이스를
구현한 프록시 객체를 **런타임에 자동 생성**한다.
```java
class LogInvocationHandler implements InvocationHandler {
    private final Object target;
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("--> " + method.getName() + " 시작");
        Object result = method.invoke(target, args);   // 진짜 객체에 위임(리플렉션)
        System.out.println("<-- " + method.getName() + " 종료");
        return result;
    }
}
OrderService proxy = (OrderService) Proxy.newProxyInstance(
        loader, new Class[]{OrderService.class}, new LogInvocationHandler(target));
```
- 동작: 클라이언트가 프록시 메서드를 부르면 → 자바가 **모든 호출을 핸들러의 `invoke`로** 보냄 → 핸들러가
  부가 기능 후 위임. 프록시 메서드를 손으로 안 짜도 된다.
- **핵심 이점**: "구현체가 100개여도 핸들러 1개". 실제로 같은 `LogInvocationHandler` 하나로 `OrderService`와
  `MemberService`(서로 다른 인터페이스) 프록시를 둘 다 만들 수 있다.
- **전제**: 프록시가 '인터페이스를 구현'하는 방식이라 **인터페이스가 반드시 있어야** 한다.

### 1-3. CGLIB — 구체 클래스 상속, 인터페이스 없어도 OK
현실엔 인터페이스 없이 구체 클래스만 있는 경우가 많다. CGLIB는 바이트코드를 조작해 **대상 클래스를 상속한
자식 클래스**를 런타임에 만들어 프록시로 쓴다.
```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(ConcreteOrderService.class);     // 이 클래스를 '상속'
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("--> " + method.getName() + " 시작");
    Object result = proxy.invokeSuper(obj, args);       // 부모(진짜) 메서드 호출 = 위임
    System.out.println("<-- " + method.getName() + " 종료");
    return result;
});
ConcreteOrderService proxy = (ConcreteOrderService) enhancer.create();
```
- 인터페이스가 없어도 동작한다(상속이라서). 핸들러는 `MethodInterceptor`, 위임은 `proxy.invokeSuper(...)`.
- **한계 — final 불가**: 상속+오버라이드로 동작하므로 **final 클래스/메서드는 프록시할 수 없다**(오버라이드
  불가). 실측에서 일반 메서드 `order()`엔 로그가 붙지만 **final 메서드 `pay()`엔 로그가 안 붙었다**(부가 기능 미적용).
- **★ JPA 연결**: 이것이 PART 14에서 'JPA 엔티티에 final 클래스/메서드를 금지'하는 이유다. JPA(Hibernate)는
  지연 로딩 등을 위해 엔티티를 CGLIB로 상속한 프록시로 다루는데, final이면 프록시를 못 만든다.

### 1-4. JDK 동적 프록시 vs CGLIB / 그리고 다음(12.7)
| | JDK 동적 프록시 | CGLIB |
|---|---|---|
| 전제 | 인터페이스 필요 | 구체 클래스로 OK |
| 방식 | 인터페이스 구현 | 클래스 상속 |
| 핸들러 | `InvocationHandler` | `MethodInterceptor` |
| 한계 | 인터페이스 없으면 불가 | final 클래스/메서드 불가 |
| 프록시 클래스명 | `$Proxy0` 등 | `...$$EnhancerByCGLIB$$...` |

- Spring Boot 3.x는 기본적으로 **CGLIB**를 쓴다(인터페이스 유무와 무관하게 일관성·기능 때문).
- **남는 문제(12.7의 동기)**: 둘은 API가 다르다(InvocationHandler vs MethodInterceptor). 대상에 따라 무엇을
  쓸지 매번 갈라 코딩하면 번거롭다. 이 둘을 하나로 통합하고, "어디에(Pointcut) 무엇을(Advice)" 적용할지까지
  묶은 것이 스프링의 `ProxyFactory` + `Advisor`다(12.7, 이 PART의 정점).

---

## 2. 실습으로 확인하기

> - **가설**: ①Reflection으로 어떤 메서드든 동적 호출 가능하나 컴파일 안전성은 잃는다. ②JDK 동적 프록시는
>   핸들러 1개로 서로 다른 인터페이스 프록시를 자동 생성($ProxyN). ③CGLIB는 인터페이스 없이 구체 클래스를
>   상속해 프록시를 만들되 final 메서드엔 적용되지 않는다.

### 코드 (`com.study.part12_aop.s06_dynamic_proxy`)
- `OrderService`/`MemberService`(인터페이스), `RealOrderService`/`RealMemberService`(구현).
- `ConcreteOrderService`(인터페이스 없는 구체 클래스, `pay()`는 final).
- `Example1_Reflection` / `Example2_JdkDynamicProxy` / `Example3_Cglib`.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s06_dynamic_proxy.Example1_Reflection
./gradlew runStage -Pmain=com.study.part12_aop.s06_dynamic_proxy.Example2_JdkDynamicProxy
./gradlew runStage -Pmain=com.study.part12_aop.s06_dynamic_proxy.Example3_Cglib
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (Reflection): 같은 코드로 다른 클래스의 메서드 호출 + 오타는 런타임에 발견.
```
--> [RealOrderService.order] 시작 ... <-- [order] 종료
--> [RealMemberService.join] 시작 ... <-- [join] 종료
[한계] 'oder'(오타) -> NoSuchMethodException (컴파일 통과, 실행 중 발견)
```
예제2 (JDK 동적 프록시): 핸들러 1개로 두 인터페이스 프록시, 클래스는 런타임 생성.
```
프록시 실제 클래스:
  orderProxy  = jdk.proxy1.$Proxy0
  memberProxy = jdk.proxy1.$Proxy1      <- 자바가 런타임에 만든 $ProxyN
```
예제3 (CGLIB): 구체 클래스 상속 프록시, final 메서드엔 부가 기능 미적용.
```
[일반 메서드 order()] --> [order] 시작 ... <-- [order] 종료     <- 로그 붙음(오버라이드됨)
[final 메서드 pay()]      [비즈니스] 결제 처리(final 메서드)      <- begin/end 없음! (오버라이드 불가)
프록시 실제 클래스 = ...ConcreteOrderService$$EnhancerByCGLIB$$201582de
```
- ①Reflection의 유연성과 컴파일 안전성 상실, ②JDK 동적 프록시의 `$ProxyN` 자동 생성(핸들러 1개), ③CGLIB의
  클래스 상속 프록시와 **final 한계**(pay에 로그 미적용)가 모두 확인됐다. ✅ → final 한계는 JPA 엔티티 final 금지로 연결.

---

## 3. 자기 점검

- **Q. 동적 프록시가 푸는 문제(수동 프록시의 고통)는?**
  - 내 답: 수동 프록시는 대상 수만큼 거의 같은 프록시 클래스를 손으로 써야 한다(100개면 100개). 동적 프록시는
    호출을 핸들러 한 곳으로 모으고 프록시 클래스를 런타임 자동 생성해 '핸들러 1개로 대상 N개'를 처리한다.

- **Q. Reflection의 이점과 한계는? 왜 프레임워크용인가?**
  - 내 답: 이름만 알면 어떤 메서드든 동적 호출(일반화)이 이점, 메서드를 문자열로 다뤄 컴파일 시점 오류
    검출이 안 되는 게 한계(오타가 런타임에야 터짐). 안전망 대신 극한의 유연성이라 대상이 미정인 프레임워크가 쓴다.

- **Q. JDK 동적 프록시와 CGLIB의 차이는?**
  - 내 답: JDK는 인터페이스 필요(인터페이스 구현, InvocationHandler), CGLIB는 인터페이스 없이 OK(클래스 상속,
    MethodInterceptor). JDK는 인터페이스 없으면 불가, CGLIB는 final 클래스/메서드 불가. Spring Boot 3.x 기본은 CGLIB.

- **Q. CGLIB가 final을 못 다루는 게 왜 JPA와 연결되나?**
  - 내 답: CGLIB는 상속+오버라이드로 프록시를 만드는데 final은 오버라이드가 안 된다. JPA(Hibernate)는 엔티티를
    CGLIB 프록시로 다뤄(지연 로딩 등) final이면 프록시를 못 만든다. 그래서 PART 14에서 엔티티에 final을 금지한다.
