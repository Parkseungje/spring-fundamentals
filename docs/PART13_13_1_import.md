# PART 13 — 트랜잭션 심화: 13.1 @Import — 설정 클래스 결합

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.1 @Import**를 다룬다.
> PART 13(트랜잭션 심화)에 본격 진입하기 전, 스프링 설정(@Configuration)을 '결합'하는 도구인 @Import를
> 짚는다. @EnableTransactionManagement 같은 @EnableXxx의 동작 원리도 여기서 이해된다.

---

## 0. 들어가기 전에 — 핵심 용어
- **@Configuration**: 빈 정의(@Bean)를 담는 설정 클래스.
- **@ComponentScan**: 패키지를 훑어 @Component/@Service/@Configuration 등을 '자동으로' 빈 등록.
- **@Import**: 특정 설정 클래스를 '명시적으로' 끌어와 등록(`@Import({A.class, B.class})`).
- **루트 설정(root config)**: 컨테이너를 띄울 때 넘기는 시작 설정 클래스.
- **메타 애노테이션(meta-annotation)**: 다른 애노테이션 위에 붙는 애노테이션(@EnableXxx가 @Import를 품는 식).
- **@EnableXxx**: 기능을 켜는 애노테이션. 내부적으로 필요한 설정을 @Import한다(@EnableAspectJAutoProxy 등).

한 줄 그림: **@Import는 여러 @Configuration을 명시적으로 한곳에 모으는 도구다. @ComponentScan(자동 탐색)과
달리 "이 설정을 등록해"라고 콕 집는다. 스캔 대상 밖(외부 라이브러리 설정 등)을 등록할 때 쓰며, @EnableXxx도
내부적으로 @Import를 사용한다. 나아가 ImportSelector로 조건에 따라 설정을 '동적으로' import할 수 있고,
그게 Spring Boot 자동설정의 원리다.**

---

## 1. 학습 내용

### 1-1. @Import — 여러 설정을 결합
큰 애플리케이션은 설정을 여러 @Configuration으로 나눈다(도메인별·기능별). 이를 한 루트 설정으로 묶는 게 @Import다.
```java
@Configuration
@Import({AppleConfig.class, BananaConfig.class})  // 두 설정을 결합
static class MainConfig {}
```
- 컨테이너에 **루트 설정 하나(MainConfig)만** 넘겨도, @Import된 설정들의 빈이 전부 등록된다.
- 설정을 모듈처럼 조립할 수 있다(필요한 설정만 골라 import).

### 1-2. @ComponentScan(자동) vs @Import(명시)
- **@ComponentScan**: 지정 패키지를 훑어 애노테이션 붙은 클래스를 '자동으로' 찾아 등록. 편하지만 "어디서
  뭐가 등록되는지" 한눈에 안 보일 수 있다.
- **@Import**: "이 설정 클래스를 등록해"라고 '명시적으로' 지정. 무엇이 등록되는지 코드에 드러난다.
- 언제 @Import가 필요한가:
  - **외부 라이브러리의 @Configuration**: 내 컴포넌트 스캔 패키지 밖이라 자동으로 안 잡힌다 → @Import로 끌어옴.
  - **테스트 전용 빈/설정**: 특정 상황에서만 명시적으로 끼워 넣고 싶을 때.
- 실측: @Import 안 한 설정의 빈은 컨테이너에 아예 없어 `getBean` 시 `NoSuchBeanDefinitionException`이 난다.
  @Import하면 정상 등록된다. → "@Import가 없으면 그 설정은 없는 것"이라는 명시성을 보여준다.

> ★ 헷갈리는 점: "@Configuration도 @Component라 스캔되는 거 아냐?" — 맞다. 그래서 같은 패키지를
> @ComponentScan하면 자동 등록된다. @Import가 빛나는 건 '스캔 대상 밖'(외부 jar, 다른 패키지)이거나
> '자동 스캔에 의존하지 않고 명시적으로 조립'하고 싶을 때다.

### 1-3. @EnableXxx의 정체 — 내부적으로 @Import
@EnableAspectJAutoProxy(12.8), @EnableTransactionManagement(PART 13에서 곧 사용), @EnableScheduling 등
수많은 @EnableXxx는 마법이 아니라 **"필요한 설정 클래스를 @Import하는 메타 애노테이션"** 이다.
```java
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Import(GreetingConfig.class)        // ← @EnableXxx의 핵심: 설정을 import
@interface EnableGreeting {}

@Configuration
@EnableGreeting                       // 이 한 줄 = "GreetingConfig를 @Import해줘"
static class MainConfig {}
```
- 실측에서 `@EnableGreeting`만 달았는데 `GreetingService` 빈이 등록됐다. → @EnableXxx = '설정을 @Import하는 별명'.
- 그래서 @EnableTransactionManagement를 붙이면 트랜잭션 관련 설정(트랜잭션 AOP 어드바이저 등)이 import되어
  @Transactional이 동작한다(13.2~). Spring Boot는 이런 @EnableXxx 상당수를 자동 설정으로 대신 켜준다.

### 1-3b. @Import가 받는 3종 — 특히 ImportSelector (Spring Boot 자동설정의 원리)
@Import는 "@Configuration을 콕 집어 등록"이 전부가 아니다. 받을 수 있는 대상이 세 가지다.
- **@Configuration / 일반 클래스**: 그대로 빈으로 등록(1-1에서 본 것, 정적).
- **`ImportSelector`**: "등록할 설정 클래스 '이름들'(String[])을 **런타임에 계산해 반환**" → 조건/환경에 따라
  **다른 설정을 골라** import(동적).
- **`ImportBeanDefinitionRegistrar`**: 빈 정의를 코드로 직접 등록(가장 저수준).
```java
class FruitImportSelector implements ImportSelector {
    public String[] selectImports(AnnotationMetadata meta) {
        // 조건(프로퍼티/프로파일/클래스 존재 등)에 따라 import할 설정을 동적으로 결정
        return "banana".equals(System.getProperty("fruit"))
            ? new String[]{ BananaConfig.class.getName() }
            : new String[]{ AppleConfig.class.getName() };
    }
}
@Configuration @Import(FruitImportSelector.class) class MainConfig {}
```
- 실측: `fruit=apple`(기본)이면 AppleService만, `-Dfruit=banana`면 BananaService만 등록된다. 셀렉터가 반환한
  이름이 곧 import 대상이 된다.
- ★ **Spring Boot 자동설정의 정체**: `@EnableAutoConfiguration`이 `AutoConfigurationImportSelector`(ImportSelector
  구현)로, **클래스패스에 무엇이 있느냐(@ConditionalOnClass 등)** 에 따라 수많은 설정을 조건부로 import한다.
  "Spring Boot가 알아서 설정해주는 마법"의 뿌리가 바로 이 @Import(ImportSelector)다. (1-3의 @EnableXxx에서 한 발 더 나간 형태.)

> ★ 참고: @Configuration 클래스는 스프링이 CGLIB 프록시로 감싼다(full mode). 그래서 @Bean 메서드를 코드에서
> 여러 번 호출해도 매번 새 객체가 아니라 '같은 싱글톤 빈'을 돌려준다(PART 8.6). 이 점이 일반 클래스와 다르다.

### 1-4. PART 13에서의 위치
13.1은 도구 단원이다. 이어지는 13.2부터 @Transactional(선언적) vs TransactionTemplate(프로그래밍)을 비교하고,
프록시 도입 전후로 트랜잭션 코드가 어떻게 비즈니스 로직에서 분리되는지 본다. 그 트랜잭션 기능을 켜는
스위치가 @EnableTransactionManagement(= @Import 기반)이다.

---

## 2. 실습으로 확인하기

> - **가설**: ①@Import로 여러 설정을 묶으면 루트 설정 하나로 모든 빈이 등록된다. ②@Import 안 하면 그 설정의
>   빈은 없다(명시성). ③@EnableXxx는 @Import를 품은 메타 애노테이션이다.

### 코드 (`com.study.part13_tx.s01_import`)
- `Configs`(AppleConfig/BananaConfig/GreetingConfig), 서비스 3종.
- `Example1_ImportCombine` / `Example2_ImportVsNoImport` / `Example3_EnableViaImport`.
- `Example4_ImportSelector` — ImportSelector로 조건(fruit 프로퍼티)에 따라 설정을 동적 import.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s01_import.Example1_ImportCombine
./gradlew runStage -Pmain=com.study.part13_tx.s01_import.Example2_ImportVsNoImport
./gradlew runStage -Pmain=com.study.part13_tx.s01_import.Example3_EnableViaImport
./gradlew runStage -Pmain=com.study.part13_tx.s01_import.Example4_ImportSelector
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (결합):
```
apple  = Apple
banana = Banana          <- 루트 설정 하나로 두 설정의 빈 모두 등록
```
예제2 (있음/없음):
```
[@Import 없음] NoSuchBeanDefinitionException -> AppleService 빈이 컨테이너에 없다
[@Import 있음] apple = Apple
```
예제3 (@EnableXxx 원리):
```
greeting = Greeting(@EnableXxx via @Import)   <- @EnableGreeting만 달았는데 등록됨
```
예제4 (ImportSelector 동적 import):
```
현재 fruit 프로퍼티 = apple (없으면 apple 기본)
AppleService 등록? true / BananaService 등록? false   (-Dfruit=banana면 반대)
```
- 셀렉터가 반환한 이름에 따라 둘 중 하나만 등록됐다. ✅ → @Import는 동적·조건부 등록도 한다(자동설정의 원리).

- @Import의 결합·명시성·@EnableXxx 원리·ImportSelector 동적 import가 모두 확인됐다. ✅

---

## 3. 자기 점검

- **Q. @Import는 무엇이고 @ComponentScan과 어떻게 다른가?**
  - 내 답: @Import는 특정 @Configuration을 '명시적으로' 끌어와 등록한다. @ComponentScan은 패키지를 훑어
    '자동' 등록. @Import는 스캔 대상 밖(외부 라이브러리 설정 등)이나 명시적 조립이 필요할 때 쓴다.

- **Q. @Import 안 한 설정의 빈을 꺼내면?**
  - 내 답: 컨테이너에 없으므로 NoSuchBeanDefinitionException. "@Import가 없으면 그 설정은 없는 것".

- **Q. @EnableXxx는 어떻게 동작하나?**
  - 내 답: 내부에 @Import를 품은 메타 애노테이션이다. @EnableXxx를 붙이면 그것이 가리키는 설정 클래스가
    import되어 빈이 등록된다. @EnableTransactionManagement/@EnableAspectJAutoProxy도 이 방식.

- **Q. @Import는 @Configuration만 받나?**
  - 내 답: 아니다. ①@Configuration/일반 클래스(정적 등록) ②ImportSelector(등록할 설정 이름을 런타임에 동적
    반환) ③ImportBeanDefinitionRegistrar(빈 정의를 코드로 등록) 세 가지를 받는다. 특히 ImportSelector로 조건부
    동적 import가 가능하다.

- **Q. Spring Boot 자동설정은 어떻게 동작하나?**
  - 내 답: @EnableAutoConfiguration이 AutoConfigurationImportSelector(ImportSelector)로, 클래스패스 조건
    (@ConditionalOnClass 등)에 맞는 설정들을 동적으로 import한다. @Import(ImportSelector)가 자동설정의 뿌리.

- **Q. 13.1이 트랜잭션 심화(PART 13)에서 갖는 의미는?**
  - 내 답: 트랜잭션 기능을 켜는 @EnableTransactionManagement가 @Import 기반이고, 설정을 모듈로 조립하는
    토대가 @Import이기 때문에 먼저 짚는다. 이어 13.2에서 선언적/프로그래밍 트랜잭션과 프록시 도입 전후를 본다.
