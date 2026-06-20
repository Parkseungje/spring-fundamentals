# PART 13 — 트랜잭션 심화: 13.4 @PostConstruct + @Transactional 함정 (internal call의 짝)

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.4 @PostConstruct + @Transactional 함정**을 다룬다.
> 13.3이 "공간상" 프록시를 우회하는 함정(this 호출)이었다면, 13.4는 "시간상" 프록시가 생기기 '이전'에
> 실행되어 트랜잭션이 안 걸리는 함정이다. 둘은 같은 원리의 두 얼굴이다.

---

## 0. 들어가기 전에 — 핵심 용어
- **@PostConstruct**: 의존성 주입(DI)이 끝난 '직후' 자동 실행되는 초기화 메서드. (jakarta.annotation.PostConstruct)
- **빈 생성 순서**: 객체 생성 → 의존성 주입 → @PostConstruct 실행 → (빈 후처리기가) 프록시로 교체 → 컨테이너 등록.
- **빈 후처리기(BeanPostProcessor)**: 빈을 등록 직전에 가로채 프록시로 바꾸는 확장점(12.8). 자동 프록시 생성기가 이것.
- **ContextRefreshedEvent**: 컨텍스트 초기화(모든 빈 생성·프록시 교체)가 끝났을 때 발행되는 이벤트.
- **ApplicationReadyEvent**: Spring Boot에서 애플리케이션 기동이 완전히 끝났을 때 발행되는 이벤트.
- **isActualTransactionActive()**: "지금 진짜 트랜잭션 안에서 실행 중인가?"를 알려주는 관찰 도구(13.3과 동일).

한 줄 그림: **@PostConstruct는 '프록시로 교체되기 이전'에 실행된다. 그 시점엔 프록시가 없어서 @Transactional이
무시된다(트랜잭션 안 걸림). 해결은 초기화를 '컨테이너가 완성된 뒤' 시점으로 미루거나(@EventListener),
이미 프록시가 된 다른 빈에 맡기는 것이다.**

---

## 1. 학습 내용

### 1-1. @PostConstruct란 — DI 직후 초기화 훅
빈을 만들 때 "의존성이 다 주입된 뒤 한 번 초기화할 일"(캐시 예열, 기본 데이터 적재 등)이 있다. 생성자에선
아직 의존성이 다 안 채워졌을 수 있어 부적합하다. 그래서 DI 완료 '직후' 자동 호출되는 `@PostConstruct`를 쓴다.

### 1-2. ★ 함정 — @PostConstruct + @Transactional은 트랜잭션이 안 걸린다
원인은 '타이밍'이다. 스프링의 빈 생성 순서를 보자.
```
객체 생성 -> 의존성 주입 -> @PostConstruct 실행 -> 빈 후처리기가 프록시로 교체 -> 컨테이너 등록
                              ↑ 여기서 실행됨            ↑ 프록시는 '이 다음'에 생김
```
- @PostConstruct는 **프록시로 교체되기 '이전'** 에 실행된다. 그 시점엔 이 빈은 아직 '진짜 객체(target)'일 뿐
  프록시가 아니다.
- @Transactional은 프록시가 처리하는데(13.2), **프록시가 아직 없으니** @PostConstruct 안의 @Transactional은
  처리할 주체가 없어 무시된다 → 트랜잭션이 시작되지 않는다(실측: isActualTransactionActive=false).

> ★ 13.3과의 관계 — 같은 원리의 두 얼굴이다.
> - 13.3(공간): this.method() 호출이 프록시를 '옆으로' 우회.
> - 13.4(시간): @PostConstruct가 프록시 생성 '이전'에 실행되어 프록시가 아직 없음.
> 공통 명제: **"프록시를 안 거치면(또는 아직 없으면) @Transactional은 없는 것."**

### 1-3. 해결책

(1) ★ 초기화를 '컨테이너 완성 후'로 미룬다 — @EventListener (권장)
```java
@EventListener(ContextRefreshedEvent.class)   // 모든 빈 생성·프록시 교체가 끝난 뒤 발행
@Transactional
public void init() { ... }
```
- 이 이벤트 시점엔 자동 프록시 생성기가 모든 대상 빈을 이미 프록시로 바꾼 뒤다 → @Transactional 정상 적용(true).
- **Spring Boot에서는 보통 `@EventListener(ApplicationReadyEvent.class)`** 를 쓴다(같은 취지, 기동 완료 시점).
- 대안: `ApplicationRunner`/`CommandLineRunner`(기동 후 실행되는 콜백)에 초기화 로직을 둔다.

(2) 트랜잭션 작업을 '별도 빈'에 맡긴다 (13.3과 같은 처방)
```java
@PostConstruct
public void init() {
    innerService.load();   // 주입받은 '다른 빈'(이미 프록시)의 @Transactional 메서드 호출
}
```
- 내 @PostConstruct 시점에 '자기 자신'은 아직 프록시가 아니지만, **주입받은 다른 빈은 이미 프록시로 완성**돼
  있다(스프링은 의존 빈을 먼저 생성·초기화한 뒤 나를 만든다). 그 빈을 거치면 프록시를 타므로 트랜잭션 적용(true).
- 결국 13.3과 동일한 해법 — "프록시를 거치게 만든다".

### 1-4. 정리 — PART 13의 함정 두 개를 관통하는 원리
13.3·13.4는 모두 "@Transactional은 프록시가 처리한다"는 사실에서 나온다. 프록시를 안 거치면(this 우회) 또는
아직 프록시가 없으면(@PostConstruct 시점) 무조건 무시된다. 해결의 본질도 같다 — **"프록시를 거치는 경로/
시점으로 옮긴다"**(다른 빈 호출, 또는 컨테이너 완성 후 실행).

---

## 2. 실습으로 확인하기

> - **가설 1**: @PostConstruct + @Transactional은 프록시 생성 전 실행이라 트랜잭션 활성=false(함정).
> - **가설 2**: @EventListener(ContextRefreshedEvent) + @Transactional은 컨테이너 완성 후라 활성=true(해결).
> - **가설 3**: @PostConstruct에서 별도 빈(프록시)의 @Transactional 메서드를 호출하면 활성=true(해결).

### 코드 (`com.study.part13_tx.s04_postconstruct`)
- `Example1_PostConstructTrap` — @PostConstruct @Transactional init() → false.
- `Example2_EventListenerSolution` — @EventListener(ContextRefreshedEvent) @Transactional → true.
- `Example3_SeparateBeanSolution` — @PostConstruct가 별도 빈의 @Transactional 호출 → true.
- (트랜잭션 설정은 13.2의 `@Import(TxConfig.class)` 재사용. 관찰은 isActualTransactionActive().)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s04_postconstruct.Example1_PostConstructTrap
./gradlew runStage -Pmain=com.study.part13_tx.s04_postconstruct.Example2_EventListenerSolution
./gradlew runStage -Pmain=com.study.part13_tx.s04_postconstruct.Example3_SeparateBeanSolution
```

### 실행 결과 — 가설과 실제 비교 (실측)
```
[함정]   @PostConstruct init()                  -> 트랜잭션 활성? false  (프록시 생성 전이라 무시)
[해결1]  @EventListener(ContextRefreshed) init() -> 트랜잭션 활성? true   (프록시 준비된 뒤)
[해결2]  @PostConstruct -> innerService.load()    -> 트랜잭션 활성? true   (다른 빈=프록시 경유)
```
- @PostConstruct만 false, 시점을 미루거나(이벤트) 다른 빈을 거치면(별도 빈) true. ✅ → 13.3과 같은 본질.

---

## 3. 자기 점검

- **Q. @PostConstruct + @Transactional이 왜 트랜잭션이 안 걸리나?**
  - 내 답: 빈 생성 순서상 @PostConstruct는 빈 후처리기가 프록시로 교체하기 '이전'에 실행된다. 그 시점엔
    프록시가 없어 @Transactional을 처리할 주체가 없으므로 무시된다(active=false).

- **Q. 해결책 두 가지는?**
  - 내 답: ① 초기화를 컨테이너 완성 후로 미룸 — @EventListener(ContextRefreshedEvent), Boot에선
    ApplicationReadyEvent(또는 ApplicationRunner). ② @PostConstruct에서 이미 프록시인 별도 빈의
    @Transactional 메서드를 호출(13.3과 같은 처방).

- **Q. 13.3(internal call)과 13.4는 어떤 공통 원리인가?**
  - 내 답: 둘 다 "@Transactional은 프록시가 처리한다"에서 나온다. 13.3은 this 호출로 프록시를 옆으로
    우회(공간), 13.4는 프록시 생성 이전에 실행(시간). 해결도 같다 — 프록시를 거치는 경로/시점으로 옮긴다.

- **Q. @PostConstruct는 왜 생성자 대신 쓰나?**
  - 내 답: 생성자 시점엔 의존성 주입이 아직 안 끝났을 수 있다. @PostConstruct는 DI 완료 직후 실행되므로
    주입된 의존성을 안전하게 사용해 초기화할 수 있다.
