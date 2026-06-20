# PART 12 — 프록시의 진화와 Spring AOP: 12.7 ProxyFactory — JDK/CGLIB 통합 + Advisor (★ 정점)

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.7 ProxyFactory**를 다룬다. PART 12의 기술적 정점이다.
> 12.6에서 JDK 동적 프록시와 CGLIB를 직접 갈라 썼다. 스프링은 이를 `ProxyFactory`로 통합하고, "어디에(Pointcut)
> 무엇을(Advice) 적용할지"를 `Advisor`로 묶는다. 이 3대 개념(Pointcut/Advice/Advisor)이 Spring AOP의 뼈대다.

---

## 0. 들어가기 전에 — 핵심 용어
- **ProxyFactory**: 스프링이 제공하는 프록시 생성기. target에 따라 JDK/CGLIB를 자동 선택해 만들어 준다.
- **proxyTargetClass**: true면 인터페이스가 있어도 강제로 CGLIB를 쓰게 하는 옵션. Spring Boot 기본 동작.
- **Advice**: '어떤 부가 로직'(로그·시간 측정 등). JDK/CGLIB 핸들러 차이를 통합한 표준 추상.
- **MethodInvocation.proceed()**: 진짜 메서드(또는 다음 어드바이스)를 호출하는 지점. 이 앞뒤를 감싸 부가 기능을 넣는다.
- **Pointcut**: '어디에' 적용할지(메서드 필터링). 실무 표준은 `AspectJExpressionPointcut`(execution 표현식).
- **Advisor**: Pointcut + Advice를 한 쌍으로 묶은 것("이 조건의 메서드에 이 로직을 적용").

한 줄 그림: **ProxyFactory는 target만 주면 JDK/CGLIB를 알아서 골라 프록시를 만들고, 부가 로직은 'Advice'
하나로 통합해 양쪽에서 똑같이 동작시킨다. '어디에(Pointcut) + 무엇을(Advice) = Advisor'로 묶어 특정
메서드에만 적용할 수 있다. 이 3대 개념이 Spring AOP의 핵심 어휘다.**

---

## 1. 학습 내용

### 1-0. 12.6이 남긴 불편 — 두 갈래의 저수준 API
12.6에서 동적 프록시를 두 가지로 만들었다. 그런데 둘은 API가 달랐다.
- JDK 동적 프록시: `InvocationHandler`의 `invoke(proxy, method, args)`.
- CGLIB: `MethodInterceptor`의 `intercept(obj, method, args, proxy)` + `proxy.invokeSuper(...)`.

같은 '로그 추가'를 하려 해도 대상에 인터페이스가 있냐 없냐에 따라 **다른 코드를 써야** 했다. 또 "인터페이스면
JDK, 아니면 CGLIB"를 개발자가 직접 갈라 코딩해야 했다. 이 두 불편을 스프링이 ProxyFactory로 없앤다.

### 1-1. ProxyFactory — JDK/CGLIB 자동 선택
target만 넘기면 스프링이 알아서 고른다.
```java
ProxyFactory pf = new ProxyFactory(target);
pf.addAdvice(new LogAdvice());
Object proxy = pf.getProxy();
```
- target에 **인터페이스가 있으면 → JDK 동적 프록시**(`$Proxy0` 등).
- **인터페이스가 없으면 → CGLIB**(`...$$SpringCGLIB$$...`).
- `pf.setProxyTargetClass(true)` → 인터페이스가 있어도 **강제 CGLIB**.
- **Spring Boot 3.x 기본은 CGLIB**다(인터페이스 유무와 무관한 일관성, 그리고 구체 클래스 주입 등 편의 때문).

→ 12.6처럼 JDK/CGLIB를 직접 분기할 필요가 사라졌다. 무엇이 선택됐는지는 프록시의 실제 클래스 이름으로 확인된다.

### 1-2. Advice — 핸들러 API 통합
ProxyFactory는 부가 로직을 'Advice'라는 하나의 추상으로 받는다. 개발자는 표준 인터페이스
`org.aopalliance.intercept.MethodInterceptor` **하나만** 구현하면 된다.
```java
public class LogAdvice implements MethodInterceptor {   // org.aopalliance.intercept
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("--> " + invocation.getMethod().getName() + " 시작");
        Object result = invocation.proceed();   // 진짜 메서드 호출(위임). JDK/CGLIB 무관 동일.
        System.out.println("<-- 종료");
        return result;
    }
}
```
- 이 Advice 하나를 JDK로 감싸진 target에도, CGLIB로 감싸진 target에도 **그대로** 적용할 수 있다(통합).
- `invocation.proceed()`는 12.3 템플릿 콜백에서 콜백을 실행하던 자리와 같은 역할 — '변하지 않는 흐름' 사이의
  '변하는 실행 지점'이다. 여러 Advice가 있으면 proceed()가 다음 Advice로, 마지막에 진짜 메서드로 이어진다(체인).

> ★ 헷갈리는 점 — 'MethodInterceptor'가 두 개다.
> - `org.aopalliance.intercept.MethodInterceptor` ← 스프링 Advice 표준(우리가 구현). 인자: `MethodInvocation`.
> - `org.springframework.cglib.proxy.MethodInterceptor` ← 12.6에서 CGLIB를 직접 쓸 때의 저수준 것.
> ProxyFactory를 쓰면 저수준(CGLIB/JDK)은 스프링이 처리하고, 우리는 aopalliance 쪽만 작성한다. import를 혼동 말 것.

### 1-3. 3대 개념 — Pointcut + Advice = Advisor ⭐
`addAdvice`는 '모든 메서드'에 부가 기능을 붙인다. 하지만 실무에선 "특정 메서드에만" 적용하고 싶다. 그
'어디에'를 담당하는 게 Pointcut이고, Pointcut + Advice를 묶은 게 Advisor다.

| 용어 | 의미 |
|---|---|
| Pointcut | "어디에" 적용할지(메서드 필터링) |
| Advice | "어떤 로직"(부가 기능) |
| Advisor | Pointcut + Advice (한 쌍) |

```java
AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
pointcut.setExpression("execution(* order(..))");          // 어디에: 이름이 order인 메서드
DefaultPointcutAdvisor advisor =
        new DefaultPointcutAdvisor(pointcut, new LogAdvice()); // Pointcut + Advice
pf.addAdvisor(advisor);                                     // addAdvice(전부) 대신 addAdvisor(조건부)
```
- 실무 표준 Pointcut은 **`AspectJExpressionPointcut`**, 표현식 예: `execution(* hello.proxy.app..*(..))`.
  (execution(반환타입 패키지.클래스.메서드(파라미터)) 형태로 '어디에'를 기술. 자세한 문법은 12.9~12.10.)
- 결과: order()엔 로그가 붙고 findStock()엔 안 붙는다(Pointcut이 걸러냄).

> ★ Pointcut의 내부 구조 — `ClassFilter` + `MethodMatcher`. Pointcut은 단순 '메서드 필터'가 아니라 2단으로
> 되어 있다: ①`ClassFilter`("어느 '클래스'에 적용하나") + ②`MethodMatcher`("그 클래스의 '어느 메서드'냐").
> 그래서 `execution(* com.x.OrderService.order(..))`는 '클래스(OrderService) + 메서드(order)' 양쪽을 모두
> 거른다. 둘 중 하나라도 안 맞으면 적용 안 됨.
>
> ★ Pointcut = AspectJ가 아니다 — `AspectJExpressionPointcut`은 여러 구현 중 '실무 표준'일 뿐이다. AspectJ
> 표현식 없이 **메서드 이름만으로** 매칭하는 `NameMatchMethodPointcut`(가장 단순)도 있다.
> ```java
> NameMatchMethodPointcut pc = new NameMatchMethodPointcut();
> pc.addMethodName("order");   // 이름이 order인 메서드만 (클래스는 전부 허용)
> ```

> ★ addAdvice vs addAdvisor — `addAdvice(advice)`는 사실 내부적으로 "항상 매칭되는 Pointcut(`Pointcut.TRUE`)
> + 그 advice"인 Advisor로 감싸 등록하는 축약형이다. 그래서 모든 메서드에 적용된다. 특정 메서드에만 걸려면
> Pointcut을 지정한 `addAdvisor`를 쓴다.

### 1-4. Spring AOP 최적화 / 다음(12.8)
- **target 1개당 프록시 1개 + 어드바이저 N개**: 여러 AOP(로그·트랜잭션·보안)를 한 빈에 동시에 적용해도 프록시는
  하나만 만들고 그 안에 어드바이저를 여러 개 체인으로 둔다(프록시 중첩이 아님). 순서는 등록 순서 또는 `@Order`.
- **체인은 '양파 껍질'처럼 돈다**: 어드바이저 2개(보안→로그)를 등록하면, 등록 순서대로 바깥→안쪽으로 진입하고
  진짜 메서드를 부른 뒤 안쪽→바깥으로 복귀한다.
  ```
  [보안 진입] -> [로그 시작] -> (진짜 메서드) -> [로그 종료] -> [보안 복귀]
  ```
  각 어드바이스의 `proceed()`가 '다음 어드바이스'를 부르고, 마지막 proceed가 진짜 메서드를 부른다. 12.6처럼
  프록시를 물리적으로 겹치는(프록시가 프록시를 감싸는) 게 아니라, **프록시 하나 안의 어드바이저 체인**이다(실측 예제4).
- **남는 문제(12.8의 동기)**: 지금은 빈마다 ProxyFactory로 프록시를 손수 만들었다. 빈이 100개면 설정 100번
  (설정 지옥). 게다가 `@Service`·`@Repository`로 컴포넌트 스캔된 '이미 만들어진' 빈은 이렇게 바꿔치기 어렵다.
  이를 자동화하는 것이 빈 후처리기(BeanPostProcessor)와 자동 프록시 생성기다(12.8).

---

## 2. 실습으로 확인하기

> - **가설**: ①ProxyFactory는 target에 따라 JDK/CGLIB를 자동 선택(옵션으로 강제 CGLIB)한다. ②같은 Advice
>   하나가 JDK·CGLIB 양쪽에서 동일하게 동작한다. ③Pointcut+Advice=Advisor로 묶으면 특정 메서드(order)에만 적용된다.

### 코드 (`com.study.part12_aop.s07_proxy_factory`)
- `OrderService`/`RealOrderService`(인터페이스+구현), `ConcreteService`(인터페이스 없음), `LogAdvice`/`SecurityAdvice`(Advice).
- `Example1_ProxyFactoryAutoSelect` / `Example2_AdviceUnified` / `Example3_PointcutAdvisor`.
- `Example4_AdvisorChainOrder` — 한 프록시에 어드바이저 2개(보안→로그) 체인 + NameMatchMethodPointcut.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s07_proxy_factory.Example1_ProxyFactoryAutoSelect
./gradlew runStage -Pmain=com.study.part12_aop.s07_proxy_factory.Example2_AdviceUnified
./gradlew runStage -Pmain=com.study.part12_aop.s07_proxy_factory.Example3_PointcutAdvisor
./gradlew runStage -Pmain=com.study.part12_aop.s07_proxy_factory.Example4_AdvisorChainOrder
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (자동 선택):
```
  (1) 인터페이스 target -> jdk.proxy1.$Proxy0                                  <- JDK
  (2) 구체 클래스 target -> ...ConcreteService$$SpringCGLIB$$0                 <- CGLIB
  (3) 인터페이스+proxyTargetClass=true -> ...RealOrderService$$SpringCGLIB$$0  <- 강제 CGLIB
```
예제2 (Advice 통합):
```
[JDK 프록시] $Proxy0          --> [order] 시작 ... <-- [order] 종료
[CGLIB 프록시] ...SpringCGLIB --> [run] 시작 ... <-- [run] 종료
```
- 같은 `LogAdvice` 하나가 JDK·CGLIB 양쪽에서 동일하게 로그를 붙였다. ✅ (12.6의 두 갈래 API 불편 해소)

예제3 (Advisor = Pointcut + Advice):
```
[order() 호출]     --> [order] 시작 ... <-- [order] 종료   <- 매칭 -> 로그 붙음
[findStock() 호출]     [비즈니스] 재고 조회                <- 불일치 -> 로그 없음
```
- order에만 begin/end가 붙고 findStock엔 안 붙었다. ✅ → Pointcut이 '어디에'를 걸러낸다.

예제4 (어드바이저 체인 순서):
```
프록시 실제 클래스 = jdk.proxy1.$Proxy0 (프록시는 '하나')
[order()]  [보안] 권한 확인(진입) -> --> [order] 시작 -> 비즈니스 -> <-- [order] 종료 -> [보안] 정리(복귀)
[findStock()]  비즈니스만(Pointcut에 없어 미적용)
```
- 보안(바깥)→로그(안쪽)→비즈니스→로그→보안 순으로 양파 껍질처럼 돈다. **프록시는 1개**, 어드바이저만 2개. ✅
  순서는 등록 순서. Pointcut은 NameMatchMethodPointcut(이름 매칭)으로도 만들었다(AspectJ 전용 아님).

---

## 3. 자기 점검

- **Q. ProxyFactory가 12.6의 무엇을 통합하나?**
  - 내 답: ①JDK/CGLIB 선택 — target에 인터페이스 있으면 JDK, 없으면 CGLIB, proxyTargetClass=true면 강제
    CGLIB(자동/강제). ②핸들러 API — InvocationHandler/MethodInterceptor를 'Advice' 하나로 통합. 개발자는
    org.aopalliance MethodInterceptor만 구현하면 양쪽에서 동작.

- **Q. Pointcut, Advice, Advisor는 각각 무엇인가?**
  - 내 답: Pointcut="어디에"(메서드 필터, 실무 표준 AspectJExpressionPointcut), Advice="무엇을"(부가 로직),
    Advisor=Pointcut+Advice(한 쌍). addAdvice는 전체 적용, addAdvisor는 Pointcut으로 조건부 적용.

- **Q. Pointcut의 내부 구조는? AspectJ만 있나?**
  - 내 답: Pointcut = ClassFilter("어느 클래스") + MethodMatcher("어느 메서드") 2단 필터. 둘 다 맞아야 적용된다.
    실무 표준은 AspectJExpressionPointcut이지만, 이름만 매칭하는 NameMatchMethodPointcut 등 다른 구현도 있다.

- **Q. 한 빈에 여러 AOP를 걸면 프록시가 여러 개 생기나? 순서는?**
  - 내 답: 아니다. 프록시는 1개고, 그 안에 어드바이저 N개를 체인으로 둔다. 등록 순서(또는 @Order)대로
    바깥→안쪽 진입, 안쪽→바깥 복귀(양파 껍질). 각 어드바이스의 proceed()가 다음으로 이어지고 마지막이 진짜 메서드.

- **Q. invocation.proceed()는 무슨 역할인가?**
  - 내 답: 진짜 메서드(또는 다음 어드바이스)를 호출하는 지점. 이 앞뒤를 감싸 부가 기능을 넣는다. 여러
    Advice가 있으면 proceed가 다음으로 이어지는 체인이 된다(12.3 템플릿 콜백의 콜백 실행 자리와 같은 역할).

- **Q. ProxyFactory의 한계와, 그것이 무엇으로 이어지나?**
  - 내 답: 빈마다 손수 프록시를 만들어야 해 빈 100개면 설정 100번(설정 지옥)이고, 컴포넌트 스캔된 빈은
    바꿔치기 어렵다. 이를 자동화하는 빈 후처리기/자동 프록시 생성기가 12.8이다.
