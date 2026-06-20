# PART 12 — 프록시의 진화와 Spring AOP: 12.10 어드바이스 5종 + @Pointcut 분리

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.10 어드바이스 5종 + @Pointcut 분리**를 다룬다.
> 12.9에서 @Around 하나만 봤다. 여기서 어드바이스 5종(@Around/@Before/@AfterReturning/@AfterThrowing/@After)의
> 차이와 실행 순서, 그리고 @Pointcut으로 표현식을 분리·재사용·조합하는 법을 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **@Around**: 가장 강력. 타겟 호출 전후 + 예외 처리 + proceed 호출 여부 제어 + 반환/예외 변환까지 가능. `ProceedingJoinPoint` 인자.
- **@Before**: 타겟 호출 '전'에 실행.
- **@AfterReturning**: 타겟이 '정상 반환'한 뒤 실행(반환값 접근 가능).
- **@AfterThrowing**: 타겟이 '예외'를 던졌을 때 실행.
- **@After**: 정상이든 예외든 '항상' 실행(try-finally의 finally).
- **@Pointcut**: 포인트컷 표현식에 '이름'을 붙여 분리. 여러 어드바이스가 그 이름을 재사용(DRY).

한 줄 그림: **@Around가 만능이지만(전후·예외·proceed 제어), 단순 케이스엔 @Before/@After 계열이 의도를
더 명확히 드러낸다. 정상이면 @AfterReturning, 예외면 @AfterThrowing, 둘 다 @After. 포인트컷 표현식은
@Pointcut으로 이름 붙여 분리하면 여러 어드바이스가 재사용하고 &&/||/! 로 조합할 수 있다.**

---

## 1. 학습 내용

### 1-1. 어드바이스 5종과 정상 흐름 실행 순서
한 메서드(order)에 5종을 모두 걸고 '정상' 호출하면 순서는(스프링 기준):
```
Around(전) -> Before -> [타겟 실행] -> AfterReturning -> After -> Around(후)
```
- @Around가 가장 바깥을 감싼다(proceed 앞=Around전, proceed 뒤=Around후). 그 안에서 Before→타겟→After계열.
- 정상 반환이므로 @AfterThrowing은 실행되지 않는다.
```java
@Around("execution(* order(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    System.out.println("[Around-전]");
    Object result = pjp.proceed();   // 이 호출이 Before -> 타겟 -> AfterReturning -> After를 포함
    System.out.println("[Around-후]");
    return result;
}
@Before("execution(* order(..))") public void before() { ... }
@AfterReturning(pointcut="execution(* order(..))", returning="result") public void afterReturning(Object result) { ... }
@After("execution(* order(..))") public void after() { ... }
```

### 1-2. 예외 흐름 — @AfterThrowing vs @AfterReturning, @After(finally)
타겟이 '예외'를 던지면 순서가 달라진다.
```
Before -> [타겟 예외] -> AfterThrowing -> After
```
- @AfterReturning은 **실행되지 않는다**(정상 반환이 아니므로).
- @AfterThrowing은 던져진 예외를 받는다(`throwing="ex"`).
- @After는 정상/예외 무관하게 **항상** 실행된다(finally).
> ★ 이 '정상이면 A, 예외면 B, 둘 다 C' 분기는 PART 11.5 @Transactional의 "정상이면 commit, 예외면 rollback,
> 끝나면 정리"와 정확히 같은 자리다. 실제로 트랜잭션 AOP도 이 어드바이스 메커니즘 위에서 동작한다.

### 1-3. @Around가 가장 강력한 이유
@Around만 할 수 있는 것:
- proceed() **호출 자체를 건너뛸 수 있다**(캐시 히트면 타겟 호출 안 함 → 접근 제어, 12.4).
- 반환값을 **바꿔서** 반환할 수 있다(변환).
- 예외를 잡아 **다른 예외로 바꾸거나 삼킬** 수 있다.
- 타겟 호출 전후 모두에 코드를 둘 수 있다.

나머지 4종은 이 중 일부만 가능한 '단순 케이스용'이다. 단 단순한 일엔 @Before/@After가 **의도를 더 명확히**
드러내고 실수가 적다(@Around는 proceed()를 깜빡하면 타겟이 아예 호출 안 되는 함정도 있다).

### 1-4. @Pointcut — 표현식 분리·재사용·조합(DRY)
어드바이스마다 `"execution(* order(..))"`를 반복하면 대상이 바뀔 때 다 고쳐야 한다(중복). @Pointcut으로
이름을 붙여 분리한다.
```java
public class Pointcuts {
    @Pointcut("execution(* order(..))") public void orders() {}        // 이름 붙인 시그니처(본문 비움)
    @Pointcut("execution(* fail(..))")  public void fails() {}
    @Pointcut("orders() || fails()")    public void orderOrFail() {}   // || 조합
    @Pointcut("execution(* ..OrderService+.*(..)) && !fails()") public void everythingButFail() {} // && / ! 조합
}
```
- 어드바이스는 이름으로 참조한다. 다른 클래스의 포인트컷은 전체 경로로 참조:
  `@Before("com.study...Pointcuts.orders()")`.
- `&&`(그리고) / `||`(또는) / `!`(부정)로 조합해 정교하게 고를 수 있다.
> ★ 함정: 조합 포인트컷을 너무 넓게 잡으면(예: 패키지 전체 `..*(..)`) 애스펙트 자신의 메서드까지 매칭돼
> 스프링이 애스펙트를 프록시하려다 **순환참조 오류**가 난다. 그래서 대상을 `OrderService+`처럼 좁혀야 한다
> (실측에서 이 오류를 만나 범위를 좁혀 해결). 포인트컷은 "필요한 곳만" 좁게 거는 게 안전·성능 모두에 좋다.

### 1-5. 다음(12.11)
이 모든 Spring AOP는 '런타임 프록시' 기반이다. 그래서 내부 호출을 못 가로채는 한계가 있다(12.11, PART 13.3).
컴파일/로딩 시점에 위빙하는 순수 AspectJ와의 차이를 12.11에서 정리한다.

---

## 2. 실습으로 확인하기

> - **가설**: ①정상 호출 시 5종이 Around전→Before→타겟→AfterReturning→After→Around후 순. ②예외 시
>   AfterThrowing이 실행되고 AfterReturning은 안 됨(After는 항상). ③@Pointcut으로 분리·재사용·조합 가능.

### 코드 (`com.study.part12_aop.s10_advice_types`)
- `OrderService`/`RealOrderService`(order 정상, fail 예외), `Pointcuts`(@Pointcut 모음).
- `Example1_FiveAdvices`(정상 순서) / `Example2_AfterThrowing`(예외 분기) / `Example3_PointcutReuse`(분리·조합).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s10_advice_types.Example1_FiveAdvices
./gradlew runStage -Pmain=com.study.part12_aop.s10_advice_types.Example2_AfterThrowing
./gradlew runStage -Pmain=com.study.part12_aop.s10_advice_types.Example3_PointcutReuse
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (정상 순서):
```
[Around-전] -> [Before] -> [비즈니스] -> [AfterReturning] -> [After] -> [Around-후]
```
예제2 (예외 분기):
```
[Before] -> [비즈니스] 오류 -> [AfterThrowing] 예외=노트북 재고 부족 -> [After] (finally)
(AfterReturning은 실행 안 됨)
```
예제3 (포인트컷 재사용·조합):
```
[order()]  [Before] everythingButFail() / [Before] orders()   <- 둘 다 매칭
[fail()]   (orders 불일치 + everythingButFail의 !fail 로 제외) <- 부가 기능 미적용
```
- 정상/예외 분기와 포인트컷 분리·조합(&&/!)이 모두 확인됐다. ✅

---

## 3. 자기 점검

- **Q. 어드바이스 5종과 정상 흐름 순서는?**
  - 내 답: @Around/@Before/@AfterReturning/@AfterThrowing/@After. 정상 순서는 Around전→Before→타겟→
    AfterReturning→After→Around후. AfterThrowing은 예외 때만.

- **Q. 예외가 나면 AfterReturning/AfterThrowing/After 중 무엇이 실행되나?**
  - 내 답: AfterThrowing과 After가 실행되고 AfterReturning은 안 된다. After는 정상/예외 무관 항상(finally).
    이 분기가 @Transactional의 commit/rollback과 같은 자리.

- **Q. @Around가 가장 강력한 이유는?**
  - 내 답: proceed 호출 여부 제어(타겟 생략 가능), 반환값/예외 변환, 전후 모두 개입이 가능. 단 proceed를
    깜빡하면 타겟이 호출되지 않는 함정이 있어, 단순 케이스엔 @Before/@After가 의도가 더 명확.

- **Q. @Pointcut의 이점과 주의점은?**
  - 내 답: 표현식에 이름을 붙여 여러 어드바이스가 재사용(DRY), &&/||/!로 조합 가능. 주의: 너무 넓게 잡으면
    애스펙트 자신까지 매칭돼 순환참조가 날 수 있어 대상을 좁혀야 한다.
