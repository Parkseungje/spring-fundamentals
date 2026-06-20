# PART 12 — 프록시의 진화와 Spring AOP: 12.8 빈 후처리기와 자동 프록시 생성기

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.8 빈 후처리기와 자동 프록시 생성기**를 다룬다.
> 12.7에서 프록시를 빈마다 손수 만들었다. 빈이 100개면 설정 100번이고, 컴포넌트 스캔된 빈은 손댈 틈이 없다.
> 이를 자동화하는 BeanPostProcessor와 자동 프록시 생성기를 본다. @Transactional이 '저절로' 적용되는 비밀이 여기 있다.

---

## 0. 들어가기 전에 — 핵심 용어
- **빈(Bean)**: 스프링 컨테이너가 관리하는 객체.
- **BeanPostProcessor(빈 후처리기)**: 빈을 컨테이너에 등록하기 직전에 가로채 조작/교체할 수 있는 후크(확장점).
- **자동 프록시 생성기(AutoProxyCreator)**: 등록된 Advisor를 보고 매칭되는 빈을 자동으로 프록시로 바꾸는 빈 후처리기.
- **AnnotationAwareAspectJAutoProxyCreator**: 스프링이 제공하는 그 자동 프록시 생성기(이름 = 어노테이션 인식 + AspectJ 표현식 + AutoProxyCreator).
- **@EnableAspectJAutoProxy**: 위 자동 프록시 생성기를 활성화하는 어노테이션(Spring Boot는 starter-aop가 자동 적용).
- **컴포넌트 스캔**: `@Service`/`@Repository`/`@Component`가 붙은 클래스를 자동으로 빈 등록하는 것.

한 줄 그림: **BeanPostProcessor는 "빈 등록 직전에 끼어들어 객체를 바꿔치기"하는 후크다. 자동 프록시
생성기는 이 후크를 이용해, 등록된 Advisor의 Pointcut에 매칭되는 모든 빈을 자동으로 프록시로 교체한다.
그래서 개발자는 Advisor만 등록하면 되고, @Service로 스캔된 빈도 프록시가 된다 — @Transactional의 토대.**

---

## 1. 학습 내용

### 1-0. 12.7이 남긴 두 고통
- **설정 지옥**: 빈마다 ProxyFactory로 프록시를 손수 만들어 등록해야 한다. 빈이 100개면 거의 같은 설정 100번.
- **스캔된 빈은 손댈 틈이 없다**: `@Service`/`@Repository`로 컴포넌트 스캔된 빈은 컨테이너가 알아서 만들어
  등록하므로, 우리가 중간에서 프록시로 바꿔치기할 기회가 없다.

→ "빈 등록 과정 자체에 끼어들어 자동으로 프록시로 바꾸는" 메커니즘이 필요하다.

### 1-1. BeanPostProcessor — 빈 등록 직전에 가로채는 후크
스프링 컨테이너는 빈을 생성·초기화한 뒤 등록한다. BeanPostProcessor는 그 사이에 끼어드는 확장점이다.
```java
public class LogProxyBeanPostProcessor implements BeanPostProcessor {
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof OrderService) {
            ProxyFactory pf = new ProxyFactory(bean);   // 원본을 target으로
            pf.addAdvice(new LogAdvice());
            return pf.getProxy();                       // 원본 '대신' 프록시를 반환 -> 이게 빈으로 등록됨
        }
        return bean;
    }
}
```
- 핵심: `postProcessAfterInitialization`이 **다른 객체를 반환하면, 그게 진짜 빈으로 등록**된다. 여기서 원본
  대신 프록시를 반환하면 컨테이너엔 프록시가 등록되고, `getBean`도 프록시를 돌려준다(원본 바꿔치기).
- 이걸로 "컨테이너가 만든 빈을 프록시로 교체"가 가능해진다(12.7에서 못 하던 것).
- 단 한계: "어떤 타입을 어떻게 감쌀지"를 if로 직접 짜야 한다 → 더 일반화한 게 자동 프록시 생성기.

### 1-2. 자동 프록시 생성기 — Advisor만 등록하면 끝
스프링은 위 일을 대신 하는 표준 빈 후처리기 **`AnnotationAwareAspectJAutoProxyCreator`**를 제공한다
(spring-boot-starter-aop가 자동 등록; 순수 컨텍스트에선 `@EnableAspectJAutoProxy`로 활성화). 동작:
```
[자동 프록시 생성기(빈 후처리기)]
  1. 컨테이너의 '모든 Advisor 빈'을 찾는다
  2. 새로 등록될 각 빈의 메서드가 Advisor의 Pointcut에 매칭되는지 검사
  3. 매칭되면 그 빈을 프록시로 교체해 등록
```
```java
@Configuration
@EnableAspectJAutoProxy
static class Config {
    @Bean OrderService orderService() { return new RealOrderService(); }
    @Bean Advisor logAdvisor() {                       // Advisor '하나'만 등록
        AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
        pc.setExpression("execution(* order(..))");
        return new DefaultPointcutAdvisor(pc, new LogAdvice());
    }
}
```
- 개발자는 **Advisor(Pointcut+Advice) 하나만** 등록한다. 대상 빈마다 프록시 설정을 손수 할 필요가 없다(설정 지옥 해소).
- Pointcut이 적용되므로 order엔 로그가 붙고 findStock엔 안 붙는다.

### 1-3. 컴포넌트 스캔된 빈도 자동 프록시된다
자동 프록시 생성기는 '빈 후처리' 시점에 끼어들기 때문에, `@Bean`이든 `@Service`로 **컴포넌트 스캔된 빈**이든
가리지 않고 Pointcut에 매칭되면 프록시로 바꾼다. 12.7의 ProxyFactory가 손대기 어려웠던 케이스가 해결된다.
```java
@Service
public class ScannedOrderService implements OrderService { ... }   // 스캔으로 등록
// -> Advisor만 있으면 이 스캔된 빈도 자동으로 프록시가 된다
```
> ★ 이것이 바로 **@Transactional·@Cacheable·@Async가 우리 서비스 빈에 '저절로' 적용되는 메커니즘**이다.
> 스프링이 그 어노테이션을 인식하는 Advisor를 등록해 두고, 자동 프록시 생성기가 매칭되는 빈을 프록시로
> 바꿔 트랜잭션/캐싱 부가 기능을 끼운다. PART 11.5의 "@Transactional은 프록시"가 여기서 완성된다.

### 1-4. 한 가지 주의 — JDK 프록시는 인터페이스 타입으로 꺼낸다
실측에서 스캔된 `ScannedOrderService`를 프록시로 바꾸자 `getBean(ScannedOrderService.class)`가 실패했다.
JDK 동적 프록시는 인터페이스(`OrderService`)를 '구현'한 `$ProxyN`이지 `ScannedOrderService`를 '상속'한 게
아니기 때문이다. 그래서 `getBean(OrderService.class)`로 꺼내야 한다. (CGLIB라면 구체 클래스 타입으로도 꺼낼 수
있다 — Spring Boot가 기본 CGLIB를 쓰는 실용적 이유 중 하나.)

### 1-5. 다음(12.9) — @Aspect
지금은 Advisor를 `@Bean`으로 직접 조립했다(Pointcut 객체 + Advice 객체). 더 선언적으로, `@Aspect`
어노테이션과 `@Around` 메서드로 Pointcut+Advice를 한 클래스에 쓰면 스프링이 Advisor로 자동 변환한다(12.9).

---

## 2. 실습으로 확인하기

> - **가설**: ①커스텀 BeanPostProcessor로 빈을 프록시로 바꿔치기할 수 있다. ②자동 프록시 생성기는 Advisor만
>   등록하면 매칭 빈을 자동 프록시로 만든다(order만 매칭). ③컴포넌트 스캔된 @Service 빈도 자동 프록시된다.

### 코드 (`com.study.part12_aop.s08_bean_postprocessor`)
- `OrderService`/`RealOrderService`/`ScannedOrderService`(@Service), `LogAdvice`.
- `Example1_CustomBeanPostProcessor` / `Example2_AutoProxyCreator` / `Example3_ComponentScannedBean`.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s08_bean_postprocessor.Example1_CustomBeanPostProcessor
./gradlew runStage -Pmain=com.study.part12_aop.s08_bean_postprocessor.Example2_AutoProxyCreator
./gradlew runStage -Pmain=com.study.part12_aop.s08_bean_postprocessor.Example3_ComponentScannedBean
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (커스텀 후처리기):
```
    [후처리기] 빈 'orderService'(RealOrderService)을 프록시로 교체
getBean으로 꺼낸 실제 클래스 = jdk.proxy2.$Proxy11   <- 원본이 프록시로 바뀜
--> [order] 시작 ... <-- [order] 종료
```
예제2 (자동 프록시 생성기 + Advisor):
```
실제 클래스 = jdk.proxy2.$Proxy15
[order()]   --> [order] 시작 ... <-- [order] 종료      <- 매칭 -> 로그
[findStock()]   [비즈니스] 재고 조회                    <- 불일치 -> 로그 없음
```
- Advisor 하나만 등록했는데 빈이 프록시가 됐고 order만 로그가 붙었다. ✅ (설정 지옥 해소)

예제3 (컴포넌트 스캔된 빈):
```
스캔된 빈의 실제 클래스 = jdk.proxy2.$Proxy19   <- @Service 스캔 빈도 프록시
[order()]  --> [order] 시작 ... (스캔된 빈) ... <-- [order] 종료
```
- `@Service`로 스캔된 빈도 자동 프록시 생성기가 프록시로 바꿨다. ✅ → @Transactional이 저절로 적용되는 원리.

---

## 3. 자기 점검

- **Q. BeanPostProcessor란 무엇이고 프록시와 무슨 관계인가?**
  - 내 답: 빈을 컨테이너에 등록하기 직전에 가로채 조작/교체하는 후크. postProcessAfterInitialization에서 원본
    대신 프록시를 반환하면 컨테이너엔 프록시가 등록된다(원본 바꿔치기). 12.7이 못 하던 "컨테이너가 만든 빈 교체"가 가능.

- **Q. 자동 프록시 생성기는 무엇을 자동화하나?**
  - 내 답: 등록된 Advisor들을 찾아, 각 빈이 Pointcut에 매칭되면 프록시로 교체한다. 개발자는 Advisor만 등록하면
    되고 빈마다 프록시 설정을 안 해도 된다(설정 지옥 해소). 스프링 빈 = AnnotationAwareAspectJAutoProxyCreator.

- **Q. 컴포넌트 스캔된 @Service 빈도 프록시가 되는 이유는?**
  - 내 답: 자동 프록시 생성기가 '빈 후처리' 시점에 끼어들기 때문에 @Bean이든 스캔된 빈이든 매칭되면 프록시로
    바꾼다. 이것이 @Transactional/@Cacheable이 우리 서비스 빈에 저절로 적용되는 메커니즘이다.

- **Q. 스캔된 빈을 프록시로 바꿨더니 getBean(구체클래스)가 실패한 이유는?**
  - 내 답: JDK 동적 프록시는 인터페이스를 구현한 $ProxyN이지 구체 클래스를 상속한 게 아니라서. 인터페이스
    타입으로 꺼내야 한다. CGLIB라면 구체 클래스 타입으로도 가능(Spring Boot 기본 CGLIB의 실용적 이점).
