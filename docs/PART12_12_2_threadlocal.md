# PART 12 — 프록시의 진화와 Spring AOP: 12.2 ThreadLocal — 싱글톤 환경의 동시성 해결

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.2 ThreadLocal**을 다룬다.
> 12.1에서 만든 로그 추적기에 "트랜잭션 ID·호출 깊이"라는 상태를 넣으려 하면, 싱글톤 빈을 여러 요청이
> 동시에 쓰면서 상태가 섞이는 문제가 생긴다. 그 문제와 해법(ThreadLocal)을 본다. (PART 7 동시성 + PART 8 싱글톤의 교차점)

---

## 0. 들어가기 전에 — 핵심 용어
- **싱글톤(Singleton)**: 인스턴스가 딱 하나만 존재하는 객체. Spring 빈은 기본이 싱글톤이다(PART 8.5).
- **상태(state)**: 객체가 필드에 들고 있는 값. 여러 호출 사이에 유지된다.
- **무상태(stateless)**: 공유되는 가변 필드가 없는 상태. 싱글톤은 무상태여야 스레드에 안전하다(PART 8.5).
- **경쟁 상태(race condition)**: 여러 스레드가 같은 가변 데이터를 동시에 읽고 써서 결과가 꼬이는 현상(PART 7.4).
- **ThreadLocal**: 같은 변수를 선언해도 '스레드마다 독립된 칸'을 주는 저장소. `set/get`은 호출한 스레드의 칸에만 작용.
- **스레드 풀(Thread Pool)**: 스레드를 미리 만들어 빌려주고 '재사용'하는 저장소(PART 7.8). 톰캣이 요청 처리에 쓴다.
- **traceId / 호출 깊이(depth)**: 로그 추적기가 "이 요청"을 식별하고 중첩 호출을 들여쓰기로 표현하기 위한 상태.
- **약한 참조(WeakReference)**: GC가 회수해도 되는 참조(강한 참조는 살아 있으면 회수 못 함). ThreadLocalMap의 키가 이것.
- **stale entry**: 키(ThreadLocal)는 GC됐는데 값은 남아 있는 '유령 엔트리'. 메모리 누수의 원인.
- **InheritableThreadLocal**: 자식 스레드 생성 시 부모의 값을 자식 칸으로 복사해 전파하는 ThreadLocal.

한 줄 그림: **상태를 가진 싱글톤을 여러 스레드가 동시에 쓰면 필드가 섞인다. ThreadLocal은 그 상태를
'스레드마다 따로' 저장해 잠금(synchronized) 없이 섞임을 막는다. 단 스레드 풀이 스레드를 재사용하므로
요청 끝에 반드시 remove()로 칸을 비워야 한다(안 그러면 다음 요청에 이전 데이터가 새고, 메모리도 누수된다).
또 값은 자식 스레드로 전파되지 않는다(필요하면 InheritableThreadLocal).**

---

## 1. 학습 내용

### 1-1. 문제 — 로그 추적기에 '상태'를 넣으면 싱글톤이 깨진다
12.1의 `LogTracer`는 호출마다 `TraceStatus`를 새로 만들어 인자로 넘겼다. 그런데 실무에선 "지금 처리 중인
요청의 traceId와 호출 깊이"를 메서드들 사이에서 공유하고 싶다(중첩 호출도 같은 traceId로 묶고, 깊이만큼
들여쓰기). 그래서 그 상태를 **Tracer의 필드**에 들고 있게 만들면 편해 보인다.

문제는 Spring 빈이 **싱글톤**(인스턴스 하나)이라는 점이다. 그 하나를 **여러 요청(여러 스레드)이 동시에**
쓰면, traceId/depth 필드를 서로 덮어쓴다.
```
[요청A 스레드] tracer.begin("A")  -> 필드 traceId = "A"
[요청B 스레드] tracer.begin("B")  -> 같은 필드 traceId = "B" (A를 덮어씀!)
[요청A 스레드] tracer.log(...)     -> 필드를 읽으면 "B"가 나옴 (A 로그에 B의 ID가 찍힘)
```
이것이 PART 8.5 "싱글톤은 무상태여야 한다"의 반례이자, PART 7.4 경쟁 상태다. 공유 가변 필드 = 동시성 폭탄.

> ★ "그럼 요청마다 Tracer를 new 하면 되지 않나?" — 그러면 싱글톤의 이점(빈 하나 재사용, DI로 어디서나
> 주입)을 버리게 된다. 또 한 요청을 처리하는 여러 객체(컨트롤러·서비스·DAO)가 '같은 Tracer'를 주입받아
> 같은 traceId를 공유해야 로그가 하나로 묶인다. 그래서 "싱글톤은 유지하되, 상태만 스레드별로 분리"하는
> ThreadLocal이 정답이다.

### 1-2. 해결 — ThreadLocal: '스레드마다 다른 칸'
ThreadLocal은 같은 변수를 선언해도 **스레드마다 독립된 저장 칸**을 준다. A 스레드가 `set`한 값은 A만
`get`하고, B는 B의 값만 본다. **공유 자체를 안 하므로** synchronized로 잠그지 않아도 안전하다.
```java
private final ThreadLocal<String> traceId = ThreadLocal.withInitial(() -> "(none)");

traceId.set("A-123");   // 내 스레드의 칸에만 저장
traceId.get();          // 내 스레드의 칸에서만 읽음 (다른 스레드 영향 없음)
```
- 잠금(synchronized/Lock, PART 7.5)은 "한 명씩 줄 세워" 공유 자원을 보호하는 방식이라 느려질 수 있다.
- ThreadLocal은 "각자 자기 것만 쓰게" 해서 **공유를 없애는** 방식이라 잠금이 필요 없다. 동시성 문제의 두
  접근(상호 배제 vs 비공유) 중 비공유 쪽이다.

비유: 학교 사물함. 변수 이름(ThreadLocal 객체)은 '사물함 줄' 하나지만, 학생(스레드)마다 자기 칸만 열고
닫는다. 같은 줄을 봐도 내용물은 학생마다 다르다.

> ★ 어떻게 스레드마다 다른 값이 나오나(원리) — `ThreadLocal.set`은 값을 ThreadLocal 객체에 저장하는 게
> 아니라 **'현재 스레드(Thread 객체)' 안의 맵(ThreadLocalMap)에 [이 ThreadLocal -> 값]으로 저장**한다.
> `get`도 현재 스레드의 맵에서 찾는다. 그래서 스레드가 다르면 들여다보는 맵 자체가 달라 값도 다르다.
> (= 값이 '변수'가 아니라 '실행 중인 스레드'에 매달려 있다.)

### 1-3. ★ 치명적 함정 — 스레드 풀 재사용 + remove() 누락 = 데이터 누수
ThreadLocal은 '스레드별' 저장소인데, **스레드 풀(PART 7.8)은 스레드를 죽이지 않고 재사용**한다(톰캣이
요청마다 풀에서 스레드를 빌려 처리하고 반납). 그래서 요청이 끝나도 ThreadLocal 값을 안 지우면, **같은
스레드가 다음 요청을 처리할 때 이전 요청의 값이 그대로 남아 있다.**
```
[풀의 스레드#7] 요청A 처리 -> traceId = "A" 저장, 그러나 remove() 안 함 -> 반납
[풀의 스레드#7] 요청B 처리(재사용) -> traceId.get() -> "A"가 나옴! (A의 데이터가 B에게 노출)
```
로그가 섞이는 정도면 다행이고, 실무에선 ThreadLocal에 **로그인 사용자 정보·권한**을 담는 경우가 많아
(Spring Security가 그렇게 한다) **"A 사용자로 로그인한 화면이 B에게 보이는" 심각한 보안 사고**가 된다.

해결: 요청 처리가 끝나면 **반드시 `finally`에서 `remove()`** 로 칸을 비운다(예외가 나도 실행되도록 finally).
```java
try {
    tracer.begin("요청");
    ... 처리 ...
} finally {
    tracer.clear();   // 내부에서 traceId.remove(); depth.remove();
}
```
> ★ 왜 finally인가 — 처리 중 예외가 터지면 remove()를 건너뛰고, 그 스레드는 더러운 값을 가진 채 풀로
> 반납된다 → 다음 요청에 누수. finally는 예외 여부와 무관하게 실행되므로 "무조건 비우기"를 보장한다.
> (PART 10.2에서 본 "커넥션은 반드시 finally에서 반납"과 똑같은 사고방식 — 빌린 건 무조건 정리.)

### 1-4. ★ 또 하나의 누락 위험 — 메모리 누수 (약한 참조와 stale entry)
remove() 누락은 1-3의 '데이터 누수(보안)'만 일으키는 게 아니라 **메모리 누수**도 일으킨다. 원리를 보자.
- `ThreadLocalMap`의 구조: **키 = ThreadLocal 객체에 대한 '약한 참조(WeakReference)'**, **값 = '강한 참조'**.
  (약한 참조 = GC가 회수해도 되는 참조. 강한 참조 = 살아 있으면 GC가 못 건드림.)
- ThreadLocal 객체 자체가 더 이상 안 쓰여 GC되면 **키는 null**이 된다. 하지만 **값은 강한 참조라, 그 스레드가
  살아 있는 한 안 치워지고 남는다**(이것을 stale entry = 키 없는 유령 값이라 한다).
- 스레드 풀은 스레드를 오래(앱 내내) 살려 둔다 → stale entry가 계속 쌓여 **메모리 누수**가 된다.
> ★ 헷갈리는 점 — "키가 약한 참조면 알아서 정리되는 거 아닌가?" → 아니다. 정리되는 건 '키'뿐이고 '값'은
> 강한 참조라 남는다. JDK가 일부 시점에 stale entry를 청소하긴 하지만 보장되지 않는다. **확실한 해법은
> 결국 `remove()`**. 그래서 remove()는 '보안(데이터 누수)' + '메모리(누수)' 두 이유로 필수다.

### 1-5. ★ 상속 미전파 — 자식 스레드와 InheritableThreadLocal
ThreadLocal은 '그 스레드의 칸'에만 값을 둔다. 그래서 **부모 스레드에서 set한 값은 자식 스레드로 전파되지
않는다**(자식은 자기 칸이 비어 초기값을 본다). 비동기/병렬 처리에서 "부모에서 넣은 traceId가 자식 작업에서
갑자기 사라지는" 흔한 함정이다.
```java
ThreadLocal<String> plain = ThreadLocal.withInitial(() -> "(none)");
plain.set("부모-traceId");
new Thread(() -> plain.get()).start();   // 자식: "(none)" — 부모 값 안 보임
```
- 해결: **`InheritableThreadLocal`** — 자식 스레드를 '생성하는 시점'에 부모의 값을 자식 칸으로 복사해 준다.
  ```java
  ThreadLocal<String> inheritable = new InheritableThreadLocal<>();
  inheritable.set("부모-traceId");
  new Thread(() -> inheritable.get()).start();   // 자식: "부모-traceId" (이어받음)
  ```
- ★ 한계: 어디까지나 '자식 생성 시점 복사'다. **스레드 풀**은 이미 만들어진 스레드를 재사용하므로 '생성 시점'이
  없어 복사가 안 일어난다 → 풀 환경에선 InheritableThreadLocal도 한계가 있어, 비동기 전파엔 별도 도구
  (데코레이터로 컨텍스트 복사 등)를 쓴다. (실측: 일반 ThreadLocal은 자식에서 초기값, InheritableThreadLocal은 부모 값.)

### 1-6. 12.1·앞으로와의 연결
- 12.1: 로그 추적기를 만들었지만 '흩어진 호출'이 문제였다 → 이후 프록시/AOP로 분리(12.4~).
- 12.2(여기): 그 추적기에 상태(traceId·깊이)를 안전하게 담는 토대를 ThreadLocal로 마련.
- 이후: 트랜잭션도 "현재 트랜잭션"을 ThreadLocal로 보관해 같은 스레드 안에서 공유한다(Spring의
  `TransactionSynchronizationManager`가 ThreadLocal 기반). 즉 @Transactional의 밑바닥에도 이 개념이 있다.

---

## 2. 실습으로 확인하기

> - **가설**: ①상태를 필드에 둔 싱글톤 Tracer를 여러 스레드가 동시에 쓰면 traceId가 섞인다. ②ThreadLocal로
>   바꾸면 (잠금 없이) 안 섞인다. ③스레드 풀 재사용 시 remove()를 빼먹으면 이전 요청 값이 다음 요청에 샌다.

### 코드 (`com.study.part12_aop.s02_threadlocal`)
- `FieldBasedTracer` — traceId/depth를 인스턴스 필드에 둔 버전(문제).
- `ThreadLocalTracer` — traceId/depth를 ThreadLocal에 둔 버전 + `clear()`(remove).
- `Example1_FieldStateRace` — 싱글톤 필드 Tracer를 3스레드 동시 사용 → 섞임 재현.
- `Example2_ThreadLocalIsolation` — 똑같은 시나리오인데 ThreadLocal로 → 섞임 없음(대조).
- `Example3_ThreadLocalLeakInPool` — 스레드 1개 풀로 재사용 강제 → remove() 누락 시 누수, finally clear() 시 해결.
- `Example4_InheritableThreadLocal` — 일반 ThreadLocal은 자식 스레드에 전파 안 됨 / InheritableThreadLocal은 부모 값 이어받음.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s02_threadlocal.Example1_FieldStateRace
./gradlew runStage -Pmain=com.study.part12_aop.s02_threadlocal.Example2_ThreadLocalIsolation
./gradlew runStage -Pmain=com.study.part12_aop.s02_threadlocal.Example3_ThreadLocalLeakInPool
./gradlew runStage -Pmain=com.study.part12_aop.s02_threadlocal.Example4_InheritableThreadLocal
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (필드 = 섞임):
```
[요청1-스레드] traceId=요청1-441 |begin
[요청2-스레드] traceId=요청1-441 |begin     <- 요청2인데 요청1의 traceId!
[요청3-스레드] traceId=요청1-441 |begin     <- 요청3도 요청1의 traceId!
[요청2-스레드] traceId=요청1-441   |처리 중
```
- 세 스레드가 **같은 traceId(요청1-441)** 를 본다. 공유 필드를 마지막에 쓴 값으로 전부 덮였다. ✅(문제 확인)

예제2 (ThreadLocal = 안 섞임):
```
[요청3-스레드] traceId=요청3-337 |begin
[요청1-스레드] traceId=요청1-816 |begin
[요청2-스레드] traceId=요청2-200 |begin
[요청1-스레드] traceId=요청1-816   |처리 중   <- begin과 log의 traceId가 일관
```
- 각 스레드가 **자기 traceId**를 끝까지 유지한다(섞임 0). synchronized 없이 안전. ✅

예제3 (풀 재사용 누수 → 해결):
```
== [함정] remove() 안 함 ==
[pool-1-thread-1] traceId=사용자A요청-600     |B 처리(나는 begin도 안 했는데...)  <- A의 값이 B에게 샘!
== [해결] finally에서 clear() ==
[pool-2-thread-1] traceId=(none) |B 처리(초기값이어야 정상)                       <- 누수 없음
```
- remove()를 빼먹으면 같은 스레드를 재사용한 B가 A의 traceId를 본다(보안 사고). finally에서 clear()하면
  B는 초기값 `(none)`만 본다. ✅ → **remove()는 선택이 아니라 필수.**

예제4 (자식 스레드 전파):
```
[main(부모)] plain=부모-traceId, inheritable=부모-traceId
[child(자식)] plain       = (none)        <- 일반 ThreadLocal: 부모 값 안 보임
[child(자식)] inheritable = 부모-traceId   <- InheritableThreadLocal: 부모 값 이어받음
```
- 일반 ThreadLocal 값은 자식 스레드로 전파되지 않고(초기값), InheritableThreadLocal만 부모 값을 복사받는다. ✅
  (단 스레드 풀 재사용에는 한계 — 비동기 전파는 별도 도구 필요.)

---

## 3. 자기 점검

- **Q. 상태를 가진 싱글톤이 왜 위험한가?**
  - 내 답: 인스턴스가 하나뿐인데 여러 스레드(요청)가 그 공유 필드를 동시에 읽고 써서 값이 섞이기 때문
    (경쟁 상태). 그래서 싱글톤은 무상태여야 안전하다(PART 8.5). 예제1에서 세 스레드가 같은 traceId를 봤다.

- **Q. ThreadLocal은 어떻게 동시성 문제를 푸나? synchronized와 뭐가 다른가?**
  - 내 답: 상태를 '스레드마다 따로' 저장해 공유 자체를 없앤다. synchronized는 공유 자원을 "한 명씩 줄 세워"
    보호(상호 배제)하지만, ThreadLocal은 "각자 자기 것만" 써서 잠금이 아예 필요 없다(비공유). 원리상
    값이 ThreadLocal이 아니라 현재 스레드(Thread)의 맵에 [ThreadLocal->값]으로 저장된다.

- **Q. remove()를 안 하면 무슨 일이 생기나? 왜 finally에 두나?**
  - 내 답: 스레드 풀이 스레드를 재사용하므로, 안 지우면 다음 요청이 같은 스레드의 이전 값을 보게 된다
    (로그 섞임을 넘어 다른 사용자 인증/데이터 노출 = 보안 사고). 처리 중 예외가 나도 무조건 비우도록
    finally에 둔다(커넥션을 finally에서 반납하는 것과 같은 사고).

- **Q. remove()를 안 하면 메모리 누수도 나나? 키가 약한 참조인데 왜?**
  - 내 답: 난다. ThreadLocalMap은 키만 약한 참조, 값은 강한 참조다. ThreadLocal이 GC돼 키가 null이 돼도
    값은 스레드가 살아 있는 한 남는다(stale entry). 스레드 풀은 스레드가 오래 살아 stale entry가 쌓여 메모리
    누수가 된다. 확실한 해법은 remove(). 그래서 remove()는 보안+메모리 두 이유로 필수.

- **Q. ThreadLocal 값은 자식 스레드에 전파되나?**
  - 내 답: 안 된다. 자식은 자기 칸만 보므로 부모가 set한 값이 안 보인다(초기값). InheritableThreadLocal을
    쓰면 자식 '생성 시점'에 부모 값을 복사해 전파한다. 단 스레드 풀(재사용)은 생성 시점이 없어 한계라,
    비동기 전파는 별도 도구가 필요하다.

- **Q. ThreadLocal은 PART 12의 AOP/트랜잭션과 어떻게 연결되나?**
  - 내 답: 로그 추적기의 traceId·깊이를 안전히 보관하는 토대이며, Spring의 트랜잭션 동기화
    (TransactionSynchronizationManager)도 "현재 트랜잭션"을 ThreadLocal에 담아 같은 스레드에서 공유한다.
    즉 @Transactional의 밑바닥에도 이 개념이 쓰인다.
