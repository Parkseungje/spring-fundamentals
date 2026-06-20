# PART 13 — 트랜잭션 심화: 13.3 Internal call 함정 (★★★ 면접 단골)

> 이 문서는 커리큘럼 PART 13의 소단원 중 **13.3 Internal call 함정**을 다룬다.
> "분명 @Transactional을 붙였는데 트랜잭션이 안 걸려요"의 1순위 원인. 12.11에서 본 'Spring AOP는 내부
> 호출을 못 가로챈다'는 한계가 @Transactional에서 실제로 어떤 사고를 내는지, 그리고 해결법을 본다.

---

## 0. 들어가기 전에 — 핵심 용어
- **@Transactional**: 메서드에 트랜잭션 경계를 거는 어노테이션. 프록시가 시작/커밋/롤백을 대신 처리(13.2).
- **프록시(proxy)**: 진짜 객체(target)를 감싼 별개 객체. 외부 호출이 프록시를 거쳐야 어드바이스(트랜잭션)가 작동(12.4·12.11).
- **내부 호출(self-invocation)**: 한 객체의 메서드가 같은 객체의 다른 메서드를 `this.method()`로 부르는 것.
- **target(진짜 객체)**: 프록시가 감싸고 있는 원본 객체. 메서드 안의 `this`가 가리키는 것이 바로 이 target.
- **isActualTransactionActive()**: "지금 진짜 트랜잭션 안에서 실행 중인가?"를 알려주는 스프링 유틸(관찰 도구).

한 줄 그림: **@Transactional은 프록시가 처리한다. 그런데 같은 클래스 안에서 `this.다른메서드()`로 부르면
그 호출은 프록시가 아니라 target에서 바로 일어나 @Transactional이 무시된다. 해결은 그 메서드를 '다른 빈'으로
분리해 호출이 프록시를 거치게 하는 것이다.**

---

## 1. 학습 내용

### 1-1. 함정 시나리오 — this.internal()은 @Transactional을 무시한다
```java
class TxService {
    @Transactional
    public void internal() { ... }     // 트랜잭션을 걸고 싶은 메서드

    public void external() {           // @Transactional 없음
        internal();                    // = this.internal() : 같은 객체 내부 호출
    }
}
```
- 클라이언트가 `txService.external()`을 부르면, external은 프록시를 거쳐 실행된다. 하지만 external 안의
  `internal()`은 사실상 **`this.internal()`** 이고, 이때 `this`는 프록시가 아니라 **진짜 객체(target)** 다.
- 그래서 internal()의 @Transactional을 처리할 프록시를 거치지 않고 target의 internal()이 그냥 실행된다
  → **트랜잭션이 시작되지 않는다.** 롤백도 안 되고, 격리/전파 설정도 무시된다.

### 1-2. 왜 그런가 — 12.11의 한계 그대로
12.11에서 본 그대로다: 프록시와 target은 **별개 객체**이고, target 내부의 `this`는 자기 자신(프록시의
존재를 모름)이라 `this.method()`는 프록시를 우회한다.
```
[클라이언트] -> [프록시] -> [target.external()] -> this.internal()
                                                   └ target에서 직접 실행(프록시 우회) -> @Transactional 무시
```
- 외부에서 `txService.internal()`을 직접 부르면? 그건 프록시를 거치므로 정상 적용된다(아래 실측 예제1).
- 즉 **같은 internal()인데 '호출 경로'(외부 직접 vs 내부 this)에 따라 @Transactional 적용 여부가 갈린다.**

> ★ 어떻게 '안 걸린 것'을 확인하나 — `TransactionSynchronizationManager.isActualTransactionActive()`로
> 메서드 안에서 "지금 진짜 트랜잭션이 도는가?"를 찍어 보면 된다. 프록시를 거쳤으면 true, 내부 호출로
> 우회됐으면 false. 실측에서 예제1은 true, 예제2(내부 호출)는 false가 나온다.

### 1-3. 해결책 — 권장은 '별도 빈으로 분리'
함정의 원인은 "같은 객체 내부 호출이라 프록시를 못 거치는 것"이다. 그러니 **다른 빈을 거치게** 만들면 된다.

(1) ★ 별도 빈으로 분리 (권장)
```java
class InnerService {
    @Transactional public void doWork() { ... }   // 트랜잭션 메서드를 '다른 빈'으로
}
class OuterService {
    private final InnerService innerService;       // 다른 빈(프록시) 주입
    public void run() { innerService.doWork(); }   // 다른 빈 호출 -> 프록시를 거침 -> @Transactional 적용
}
```
- `innerService`는 프록시라, `innerService.doWork()`는 프록시를 거쳐 트랜잭션이 정상 시작된다(실측 예제3: true).
- 단순히 함정을 피하는 것을 넘어, '트랜잭션 단위 작업'을 독립 책임(빈)으로 떼어내는 것은 **SRP(단일 책임)**
  관점에서도 더 낫다. 그래서 가장 권장된다.

(2) 자기 자신을 프록시로 호출 (대안)
- `AopContext.currentProxy()`로 현재 프록시를 얻어 `((TxService) AopContext.currentProxy()).internal()` 호출.
  (`@EnableAspectJAutoProxy(exposeProxy = true)` 필요.) 또는 자기 자신을 주입(self-injection)받아 호출.
- 동작은 하지만 코드가 지저분하고 의도가 안 드러나 권장도는 낮다.

(3) AspectJ 컴파일 방식으로 전환
- 프록시가 아니라 바이트코드에 직접 위빙하므로 내부 호출도 가로챈다(12.11). 설정 비용이 커서 특수한 경우만.

### 1-4. 13.4와의 연결
같은 '프록시 타이밍' 문제의 짝이 13.4다 — `@PostConstruct`에 @Transactional을 붙이면, 그 시점엔 아직
프록시가 만들어지기 전이라 트랜잭션이 안 걸린다. 13.3(공간: this 우회)과 13.4(시간: 프록시 생성 전)는
"프록시를 안 거치면 @Transactional은 없는 것"이라는 같은 원리의 두 얼굴이다.

---

## 2. 실습으로 확인하기

> - **가설 1**: internal()을 외부에서 프록시로 직접 부르면 트랜잭션 활성=true.
> - **가설 2**: external()이 this.internal()을 부르면, @Transactional이 있어도 트랜잭션 활성=false(함정).
> - **가설 3**: @Transactional 메서드를 별도 빈으로 분리해 호출하면 프록시를 거쳐 활성=true(해결).

### 코드 (`com.study.part13_tx.s03_internal_call`)
- `TxService` — external()(@Transactional 없음) + internal()(@Transactional), isActualTransactionActive() 출력.
- `Example1_ExternalCallWorks` / `Example2_InternalCallFails` / `Example3_SeparateBean`.
- (트랜잭션 설정은 13.2의 `@Import(TxConfig.class)`로 재사용.)

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part13_tx.s03_internal_call.Example1_ExternalCallWorks
./gradlew runStage -Pmain=com.study.part13_tx.s03_internal_call.Example2_InternalCallFails
./gradlew runStage -Pmain=com.study.part13_tx.s03_internal_call.Example3_SeparateBean
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (외부 직접 호출 — 적용):
```
빈 실제 클래스 = TxService$$SpringCGLIB$$0 (프록시)
internal() 실행 -> 트랜잭션 활성? true  (적용됨)
```
예제2 (내부 호출 — 함정):
```
external() 실행 -> 트랜잭션 활성? false (external 자체는 @Transactional 없음)
-> 내부에서 this.internal() 호출:
internal() 실행 -> 트랜잭션 활성? false  (false = @Transactional 무시됨!)
```
예제3 (별도 빈 분리 — 해결):
```
OuterService.run() -> innerService.doWork() 호출(다른 빈=프록시 경유):
InnerService.doWork() -> 트랜잭션 활성? true  (적용됨!)
```
- 같은 @Transactional 메서드인데, **호출 경로**에 따라 true/false가 갈렸다. 내부 호출(this)만 false. ✅ →
  별도 빈으로 분리하면 프록시를 거쳐 다시 true(해결).

---

## 3. 자기 점검

- **Q. @Transactional internal()을 같은 클래스의 external()에서 부르면 왜 트랜잭션이 안 걸리나?**
  - 내 답: external 안의 internal()은 this.internal()이고, this는 프록시가 아니라 target이라 프록시를
    우회한다. @Transactional은 프록시가 처리하는데 프록시를 안 거치니 무시된다(isActualTransactionActive=false).

- **Q. 외부에서 internal()을 직접 부르면?**
  - 내 답: 그건 프록시를 거치므로 정상 적용된다(true). 즉 같은 메서드라도 '호출 경로'가 관건이다.

- **Q. 가장 권장되는 해결책과 그 이유는?**
  - 내 답: @Transactional 메서드를 별도 빈으로 분리해 호출이 프록시를 거치게 한다. 함정 회피뿐 아니라
    트랜잭션 작업을 독립 책임으로 떼어내 SRP에도 부합. (대안: AopContext/self-injection, AspectJ 전환.)

- **Q. 13.3과 13.4(@PostConstruct)의 공통 원리는?**
  - 내 답: "프록시를 안 거치면 @Transactional은 없는 것". 13.3은 this 호출로 프록시를 우회(공간), 13.4는
    프록시 생성 이전 시점에 실행(시간). 둘 다 프록시 메커니즘의 한계에서 나온다.
