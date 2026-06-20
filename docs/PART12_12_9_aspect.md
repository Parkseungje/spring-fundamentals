# PART 12 — 프록시의 진화와 Spring AOP: 12.9 @Aspect와 AOP 용어 7가지

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.9 @Aspect**를 다룬다.
> 12.8까지 Pointcut·Advice·Advisor를 객체로 직접 조립했다. @Aspect는 이를 선언적으로(어노테이션으로) 쓰게
> 해주며, 스프링이 @Aspect 클래스를 Advisor로 자동 변환한다. AOP의 표준 용어 7가지도 여기서 정리한다.

---

## 0. 들어가기 전에 — 핵심 용어(AOP 7대 용어)
| 용어 | 의미 |
|---|---|
| 조인 포인트(Join point) | 부가 기능을 적용할 수 있는 모든 지점(메서드 호출 등) |
| 포인트컷(Pointcut) | 조인 포인트 중 실제 적용할 곳 선택(필터) |
| 어드바이스(Advice) | 적용할 부가 기능(로직) |
| 애스펙트(Aspect) | 포인트컷 + 어드바이스의 모듈 |
| 타겟(Target) | 부가 기능이 적용되는 실제 객체 |
| 위빙(Weaving) | 포인트컷으로 어드바이스를 결합하는 과정(Spring AOP는 빈 후처리 시점) |
| AOP 프록시 | JDK 동적 프록시 또는 CGLIB |

- **@Aspect**: AspectJ의 어노테이션을 스프링이 차용. 이 클래스가 애스펙트(Pointcut+Advice 모듈)임을 표시.
- **@Around**: 포인트컷(어디에)과 어드바이스(무엇을)를 한 메서드에 결합한 어드바이스 종류.
- **ProceedingJoinPoint**: @Around가 받는 인자. 가로챈 조인 포인트의 정보(메서드·인자 등)를 담고, `proceed()`로 타겟을 호출.

한 줄 그림: **@Aspect 클래스에 @Around("execution(...)")로 '어디에+무엇을'을 선언하면, 스프링이 이를
Advisor로 자동 변환해 자동 프록시 생성기가 적용한다. 단 @Aspect는 빈 등록까지 자동은 아니다 —
@Component/@Bean으로 등록해야 동작한다.**

---

## 1. 학습 내용

### 1-1. @Aspect — Pointcut+Advice를 선언적으로
12.7~12.8에서는 `AspectJExpressionPointcut`(객체) + Advice(객체)를 `DefaultPointcutAdvisor`로 조립했다.
@Aspect는 그걸 어노테이션으로 간결히 쓴다.
```java
@Aspect
static class LogAspect {
    @Around("execution(* order(..))")            // 포인트컷(어디에) + 어드바이스(무엇을)
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("--> " + joinPoint.getSignature().getName() + " 시작");
        Object result = joinPoint.proceed();      // 타겟 실제 메서드 호출
        System.out.println("<-- 종료");
        return result;
    }
}
```
- 스프링은 `@Aspect` 클래스(빈)를 발견하면 내부의 @Around 등을 읽어 **Advisor로 자동 변환**한다. 그 Advisor를
  자동 프록시 생성기(12.8)가 매칭 빈에 적용한다. → 우리가 만든 12.7~12.8 메커니즘 위에서 동작하는 '편의 문법'이다.

### 1-2. ProceedingJoinPoint — 조인 포인트 정보 + 타겟 호출
@Around 메서드는 `ProceedingJoinPoint`를 받는다. 이건 '지금 가로챈 호출 지점(조인 포인트)'을 대표한다.
```java
joinPoint.getSignature().toShortString(); // 예: OrderService.order(..)
joinPoint.getArgs();                       // 인자 배열, 예: [노트북]
Object result = joinPoint.proceed();       // 타겟 메서드 호출(이 앞뒤를 감싸 부가 기능)
```
- `proceed()`는 12.7의 `MethodInvocation.proceed()`와 같은 역할 — 타겟(또는 다음 어드바이스)을 호출하는 지점.
- 메서드명·인자·반환값을 읽어 로깅·시간 측정·검증 등에 활용한다.
- `proceed(Object[] args)`로 **타겟에 다른 인자를 넘길** 수도 있다(인자 검증·치환 시). @Around가 강력한 이유 중 하나.

> ★ @Around 함정 — proceed()를 빠뜨리면 타겟이 아예 실행 안 된다. @Around는 '직접' 타겟을 불러야 한다.
> @Before/@After와 달리 타겟 호출 책임이 개발자에게 있어서, proceed()를 깜빡하면 **비즈니스 로직이 통째로
> 실행되지 않고 반환값도 null**이 된다(실측 예제4). 그래서 단순히 전/후 처리만 필요하면 proceed가 필요 없는
> @Before/@After가 더 안전하다(12.10).

> ★ JoinPoint vs ProceedingJoinPoint — `proceed()`는 **`ProceedingJoinPoint`에만** 있다(= @Around 전용).
> @Before/@After/@AfterReturning/@AfterThrowing은 그냥 `JoinPoint`를 받아 정보(메서드·인자)는 읽지만
> **타겟 호출을 제어할 수 없다**(스프링이 타겟을 알아서 부른다). "왜 @Around만 proceed가 있나"의 답이다(12.10).

### 1-3. AOP 7대 용어 — 코드와 매핑
위 예제에 7대 용어를 대응시키면:
- 조인 포인트 = 가로챈 그 메서드 호출 지점(`joinPoint`).
- 포인트컷 = `@Around("execution(* order(..))")`의 표현식("어디에").
- 어드바이스 = log 메서드 본문("무엇을").
- 애스펙트 = `@Aspect LogAspect`(포인트컷+어드바이스 모듈).
- 타겟 = `RealOrderService`(실제 객체).
- 위빙 = 스프링이 빈 후처리 시점에 프록시로 어드바이스를 결합(12.8).
- AOP 프록시 = 실제 생성된 프록시(`$ProxyN` 또는 CGLIB).

> ★ 조인 포인트 vs 포인트컷 — '조인 포인트'는 적용 "가능한" 모든 지점(이론상 후보 전체), '포인트컷'은 그중
> "실제로 고른" 지점(필터). Spring AOP의 조인 포인트는 항상 '메서드 호출'이다(필드 접근 등은 불가 — 프록시
> 기반이라서). 순수 AspectJ는 더 넓다(12.11).

### 1-4. ★ 함정 — @Aspect는 자동 빈 등록이 아니다
@Aspect는 "이 클래스가 애스펙트다"라는 표시일 뿐, 빈으로 자동 등록되지는 않는다. 스프링 빈으로 등록돼야
(@Component/@Bean/@Import) 자동 프록시 생성기가 Advisor로 변환해 적용한다.
```java
@Aspect class LogAspect { ... }          // 이것만으론 적용 안 됨!
@Bean LogAspect logAspect() { ... }      // 이렇게 빈 등록해야 동작 (실무에선 보통 @Component를 함께)
```
초보자가 "@Aspect 붙였는데 AOP가 왜 안 먹지?" 하는 가장 흔한 원인이다. 실측에서 미등록 시 빈이 프록시가
아니라 원본(`RealOrderService`)이라 로그가 안 붙었고, @Bean 등록 후 프록시(`$ProxyN`)가 되어 로그가 붙었다.

### 1-5. AOP는 OOP의 보조 — 남용 주의
AOP는 OOP를 '대체'하지 않고 횡단 관심사(로깅·트랜잭션·보안)를 '보조'한다. 남용하면 코드만 봐선 어떤 부가
동작이 끼는지 안 보여 흐름 추적·디버깅이 어려워진다. "여러 곳에 공통으로 반복되는 부가 기능"에만 쓰는 게 원칙.

### 1-6. 다음(12.10)
@Around 외에 어드바이스가 5종(@Before/@After/@AfterReturning/@AfterThrowing/@Around) 있고, 포인트컷을
@Pointcut으로 분리해 재사용한다(12.10).
- (참고) 애스펙트가 여러 개면 적용 순서를 `@Order`(또는 `Ordered` 구현)로 정한다. 한 애스펙트 안 여러
  어드바이스의 순서까지 보장하려면 애스펙트를 나눠 @Order를 주는 게 안전하다.

---

## 2. 실습으로 확인하기

> - **가설**: ①@Aspect+@Around 빈을 등록하면 order에 부가 기능이 붙는다. ②ProceedingJoinPoint로 메서드명·
>   인자·반환을 읽을 수 있다. ③@Aspect만 붙이고 빈 등록을 안 하면 적용되지 않는다(함정).

### 코드 (`com.study.part12_aop.s09_aspect`)
- `OrderService`/`RealOrderService`(타겟).
- `Example1_AtAspect`(@Around 기본) / `Example2_JoinPointInfo`(조인포인트 정보+7대 용어) / `Example3_AspectNotAutoRegistered`(빈 등록 함정).
- `Example4_ProceedOmittedTrap` — @Around에서 proceed() 누락 시 타겟 미실행(null) vs 호출 시 정상.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s09_aspect.Example1_AtAspect
./gradlew runStage -Pmain=com.study.part12_aop.s09_aspect.Example2_JoinPointInfo
./gradlew runStage -Pmain=com.study.part12_aop.s09_aspect.Example3_AspectNotAutoRegistered
./gradlew runStage -Pmain=com.study.part12_aop.s09_aspect.Example4_ProceedOmittedTrap
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (@Aspect 기본): order에만 @Around 로그.
```
실제 클래스 = jdk.proxy2.$Proxy17
[order()]  --> [@Aspect] order 시작 ... <-- [@Aspect] order 종료
[findStock()]  [비즈니스] 재고 조회        <- 포인트컷 불일치, 로그 없음
```
예제2 (조인 포인트 정보):
```
--> 조인포인트: OrderService.order(..) 인자=[노트북]
<-- 반환=노트북 주문완료 (814us)
```
예제3 (함정 → 해결):
```
[함정] 실제 클래스 = RealOrderService (프록시 아님)   <- @Aspect 미등록, 로그 없음
[해결] 실제 클래스 = $Proxy17 (프록시)   --> [@Aspect] 시작 ... <-- 종료   <- @Bean 등록 후 적용
```
예제4 (proceed 누락 함정):
```
[함정] --> [Around] 시작(proceed 안 함) -> <-- [Around] 종료   반환값 = null  (비즈니스 미실행!)
[정상] --> [Around] 시작 -> [비즈니스] 주문 처리 -> <-- 종료    반환값 = 노트북 주문완료
```
- proceed()를 빠뜨리면 타겟이 아예 안 불려 비즈니스가 실행 안 되고 null 반환. 호출하면 정상. ✅
- @Aspect의 자동 변환·조인포인트 활용·빈 등록 함정·proceed 누락 함정이 모두 확인됐다. ✅

---

## 3. 자기 점검

- **Q. @Aspect는 무엇이고 스프링이 어떻게 처리하나?**
  - 내 답: Pointcut+Advice를 한 클래스에 선언하는 모듈(애스펙트). 스프링이 @Aspect 빈을 Advisor로 자동
    변환하고, 자동 프록시 생성기(12.8)가 매칭 빈에 적용한다. @Around는 포인트컷+어드바이스를 한 메서드에 결합.

- **Q. ProceedingJoinPoint의 역할은?**
  - 내 답: 가로챈 조인 포인트 정보(메서드 시그니처·인자 등)를 담고, proceed()로 타겟 메서드를 호출한다.
    이 앞뒤를 감싸 부가 기능을 넣는다(12.7 MethodInvocation.proceed와 같은 역할).

- **Q. 조인 포인트와 포인트컷의 차이는?**
  - 내 답: 조인 포인트는 적용 가능한 모든 지점(후보), 포인트컷은 그중 실제 적용할 곳(필터). Spring AOP의
    조인 포인트는 항상 메서드 호출이다(프록시 기반이라 필드 접근 등은 불가).

- **Q. @Aspect를 붙였는데 AOP가 안 먹는 흔한 원인은?**
  - 내 답: @Aspect는 자동 빈 등록이 아니다. @Component/@Bean으로 빈 등록을 해야 적용된다. 미등록 시 빈이
    원본 그대로라 프록시가 안 생긴다.

- **Q. @Around에서 proceed()를 빠뜨리면?**
  - 내 답: 타겟 메서드가 아예 호출되지 않아 비즈니스 로직이 통째로 실행 안 되고 반환값도 null이 된다.
    @Around는 직접 타겟을 불러야 하는 책임이 있다. 단순 전/후 처리는 proceed가 필요 없는 @Before/@After가 안전.

- **Q. JoinPoint와 ProceedingJoinPoint의 차이는?**
  - 내 답: proceed()는 ProceedingJoinPoint(=@Around 전용)에만 있다. @Before/@After 등은 JoinPoint를 받아
    정보(메서드·인자)는 읽지만 타겟 호출을 제어할 수 없다(스프링이 타겟을 부른다). 그래서 @Around만 proceed가 있다.
