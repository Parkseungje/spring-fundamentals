# PART 12 — 프록시의 진화와 Spring AOP: 12.11 AspectJ vs Spring AOP

> 이 문서는 커리큘럼 PART 12의 마지막 소단원 **12.11 AspectJ vs Spring AOP**를 다룬다.
> 지금까지 만든 Spring AOP는 '런타임 프록시' 방식이다. 그 방식의 결정적 귀결인 '내부 호출(self-invocation)
> 한계'를 실증하고, 컴파일/로딩 시점에 위빙하는 순수 AspectJ와의 차이를 정리한다.

---

## 0. 들어가기 전에 — 핵심 용어
- **AspectJ**: AOP의 원조. 자체 컴파일러·문법을 갖고, 부가 기능을 컴파일/클래스 로딩 시점에 바이트코드에 직접 짜 넣는다(위빙).
- **Spring AOP**: AspectJ '문법(@Aspect 등)만 차용'하고, 실제 적용은 '런타임 프록시'로 한다. 별도 컴파일러 불필요.
- **위빙(Weaving)**: 부가 기능(어드바이스)을 실제 코드에 결합하는 과정. AspectJ=컴파일/로딩 시점, Spring AOP=런타임 프록시 시점.
- **내부 호출(self-invocation)**: 한 객체의 메서드가 같은 객체의 다른 메서드를 `this.method()`로 부르는 것.
- **프록시(proxy)**: 진짜 객체(target)를 감싼 별도 객체. 클라이언트는 프록시를 호출하고, 프록시가 어드바이스를 끼운 뒤 진짜 객체에 위임.

한 줄 그림: **Spring AOP는 런타임 프록시라, 진짜 객체와 '별개인' 프록시를 통해 호출될 때만 어드바이스가
붙는다. 그래서 진짜 객체 내부의 this.method() 호출은 프록시를 못 거쳐 어드바이스가 빠진다(내부 호출 한계).
컴파일/로딩 시 바이트코드에 직접 위빙하는 AspectJ는 이 한계가 없다.**

---

## 1. 학습 내용

### 1-1. 두 AOP의 차이
| | AspectJ | Spring AOP |
|---|---|---|
| 위빙 시점 | 컴파일 / 클래스 로딩 시점 | 런타임(프록시 생성 시) |
| 방식 | 바이트코드에 직접 삽입 | 프록시(JDK/CGLIB) 통해 위임 |
| 별도 도구 | 전용 컴파일러(ajc)/위버 필요 | 불필요(스프링만으로) |
| 적용 범위(조인 포인트) | 메서드·생성자·필드 접근 등 광범위 | 메서드 호출만 |
| 내부 호출 | 가로챔(O) | 못 가로챔(X) |
| 학습/설정 난도 | 높음 | 낮음(실무 대부분 충분) |

- 실무 대부분은 Spring AOP로 충분하다(메서드 단위 트랜잭션·로깅·보안). 정말 세밀한 위빙이 필요할 때만 AspectJ.
- Spring AOP는 AspectJ의 '문법'(@Aspect/@Around/execution(...))만 빌렸을 뿐, 동작 원리(런타임 프록시)는 다르다.
- AspectJ는 메서드뿐 아니라 **생성자·필드 읽기/쓰기(get/set)·static 초기화**까지 가로챈다(조인 포인트가 훨씬 넓음).
- 성능: 런타임 프록시는 호출마다 프록시를 거치는 오버헤드가 있고, 컴파일 위빙(CTW)은 코드에 직접 박혀 더 빠르다.

### 1-1b. 위빙은 3종류 — CTW / LTW / 런타임 프록시
"AspectJ=컴파일/로딩, Spring=런타임"을 더 정확히 나누면 위빙은 세 가지다.
- **컴파일 타임 위빙(CTW, Compile-Time Weaving)**: AspectJ 전용 컴파일러(`ajc`)가 **컴파일할 때** 바이트코드에
  어드바이스를 직접 짜 넣는다. 가장 빠르지만 전용 컴파일러가 필요.
- **로드 타임 위빙(LTW, Load-Time Weaving)**: 클래스가 **JVM에 로딩되는 순간** `javaagent`(위버)가 바이트코드를
  조작해 끼워 넣는다. 전용 컴파일러는 없어도 되지만 실행 시 javaagent 설정이 필요.
- **런타임(프록시) 방식**: Spring AOP가 쓰는 것. 엄밀히는 '위빙'이 아니라 **런타임에 프록시 객체로 감싸는** 것이다
  (바이트코드를 고치는 게 아니라 대리 객체를 끼움). 그래서 프록시를 거치는 호출에만 적용된다(내부 호출 한계의 근원).
- (참고) Spring에서 AspectJ LTW를 쓰려면 `@EnableLoadTimeWeaving` + javaagent를 설정한다. 보통은 안 쓰고
  기본 런타임 프록시(@EnableAspectJAutoProxy)로 충분하다.

### 1-2. ★ Spring AOP의 결정적 한계 — 내부 호출(self-invocation)
Spring AOP는 항상 '프록시'를 거쳐야 어드바이스가 동작한다. 프록시는 진짜 객체(target)를 감싼 **별개의 객체**다.
```
[클라이언트] -> [프록시] --어드바이스--> [진짜 객체.external()]   (외부 호출: 어드바이스 O)
```
그런데 진짜 객체 '안'에서 `this.external()`을 부르면, 그 `this`는 프록시가 아니라 **진짜 객체 자신**이다.
```
[클라이언트] -> [프록시] -> [진짜 객체.internalCaller()] -> this.external()
                                                            └ 진짜 객체에서 직접 호출(프록시 우회) -> 어드바이스 X
```
- 그래서 같은 `external()`이라도 **외부에서 프록시로 부르면 어드바이스가 붙고, 내부에서 this로 부르면 안 붙는다.**
- 실무 사고로 직결: 같은 서비스 클래스 안에서 `@Transactional` 메서드를 다른 메서드가 내부 호출하면 **트랜잭션이
  안 걸린다**(가장 흔한 @Transactional 함정 — PART 13.3).

### 1-3. 왜 그런가 — 프록시 ≠ 진짜 객체
원인은 "프록시와 진짜 객체가 서로 다른 두 객체"라는 데 있다. 실측으로 정체성을 비교하면:
```
클라이언트가 받은 빈.getClass() = CallService$$SpringCGLIB$$0   <- 프록시
external() 메서드 안의 this.getClass() = CallService           <- 진짜 객체
```
- 둘의 클래스가 다르다 = 별개 객체. 진짜 객체는 자기를 감싼 프록시의 존재를 모른다.
- 따라서 진짜 객체 내부의 `this.method()`는 프록시를 거칠 방법이 없다(자기 자신을 직접 부를 뿐).
- AspectJ는 프록시가 아니라 **바이트코드 자체에 어드바이스를 짜 넣으므로** 내부 호출이든 외부 호출이든 똑같이
  가로챈다(애초에 '거쳐야 할 프록시'가 없다). 이것이 두 방식의 본질적 차이다.

### 1-3b. Spring AOP의 한계 '종합' — 모두 '프록시 기반'에서 나온다
내부 호출은 한 예일 뿐, Spring AOP(런타임 프록시)에는 같은 뿌리의 한계가 여럿이다. 한곳에 모은다.

| 한계 | 이유 | AspectJ는? |
|---|---|---|
| 메서드 호출만 가로챔(생성자·필드·static 불가) | 프록시는 메서드 위임만 가로챌 수 있음 | 가능(조인 포인트가 넓음) |
| public 메서드만(private/protected 불가) | 프록시가 외부 호출로 오버라이드/위임하는 구조 | 가능 |
| 내부 호출(this.method()) 불가 | this는 프록시가 아니라 진짜 객체(1-2,1-3) | 가능 |
| final 클래스/메서드 불가 | CGLIB가 상속·오버라이드로 프록시를 만들기 때문(12.6) | 가능 |

- 공통 뿌리: **"프록시를 거쳐야만 어드바이스가 동작한다"**. 프록시가 끼어들 수 없는 지점(내부 호출·private·
  생성자·필드·final)은 전부 적용이 안 된다.
- AspectJ는 바이트코드에 직접 위빙하므로 이 한계가 전부 없다(대신 도구·설정 비용). 실무는 이 한계를 알고
  Spring AOP로 충분히 쓰되, 걸리면 구조를 바꾸거나(PART 13.3) AspectJ로 전환한다.

### 1-4. 해결책 미리보기(PART 13.3)
내부 호출 문제의 해결책은 "내부 호출도 프록시를 거치게" 만드는 것이다:
- 자기 자신을 프록시로 주입받아 호출(self-injection).
- `AopContext.currentProxy()`로 현재 프록시를 얻어 호출.
- 가장 권장: **메서드를 다른 빈(클래스)으로 분리**해 외부 호출로 만든다(구조 개선).
- 또는 AspectJ(컴파일/로딩 시 위빙)로 전환.
상세는 PART 13.3에서 다룬다.

---

## 2. 실습으로 확인하기

> - **가설**: ①외부에서 프록시로 external()을 부르면 어드바이스가 붙는다. ②internalCaller() 내부의
>   this.external()에는 어드바이스가 안 붙는다. ③빈(프록시)과 메서드 안 this(진짜 객체)의 클래스가 달라
>   둘이 별개 객체임을 확인 → 내부 호출 한계의 원인.

### 코드 (`com.study.part12_aop.s11_aspectj_vs_springaop`)
- `CallService`(external/internalCaller, this.getClass() 출력), `LogAspect`(external에 @Around).
- `Example1_ExternalCallWorks` / `Example2_InternalCallFails` / `Example3_WhyMechanism`.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s11_aspectj_vs_springaop.Example1_ExternalCallWorks
./gradlew runStage -Pmain=com.study.part12_aop.s11_aspectj_vs_springaop.Example2_InternalCallFails
./gradlew runStage -Pmain=com.study.part12_aop.s11_aspectj_vs_springaop.Example3_WhyMechanism
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (외부 호출 — 적용됨):
```
빈 실제 클래스 = ...CallService$$SpringCGLIB$$0 (프록시)
[bean.external()]  [AOP] external 시작 <<< 어드바이스 적용됨 ... [AOP] external 종료
```
예제2 (내부 호출 — 미적용):
```
[bean.internalCaller()]
    internalCaller() 실행 (this=CallService)
    -> 내부에서 this.external() 호출:
    external() 실행 (this=CallService)      <- [AOP] 로그가 '없다'! (예제1과 대조)
```
예제3 (원인 — 별개 객체):
```
빈.getClass() = CallService$$SpringCGLIB$$0   <- 프록시
external() 안 this = CallService              <- 진짜 객체 (클래스가 다름 = 별개 객체)
```
- 같은 external인데 호출 경로(외부/내부)에 따라 어드바이스 적용이 갈렸고, 그 원인이 '프록시≠진짜 객체'임이
  정체성 비교로 확인됐다. ✅ → @Transactional 내부 호출 함정의 뿌리(PART 13.3).

---

## 3. 자기 점검

- **Q. AspectJ와 Spring AOP의 핵심 차이는?**
  - 내 답: AspectJ는 컴파일/로딩 시점에 바이트코드에 직접 위빙(전용 컴파일러 필요, 조인 포인트 광범위),
    Spring AOP는 런타임 프록시로 위임(별도 도구 불필요, 메서드 호출만, 문법만 AspectJ 차용). 실무 대부분 Spring AOP로 충분.

- **Q. Spring AOP가 내부 호출을 못 가로채는 이유는?**
  - 내 답: 어드바이스는 프록시를 거칠 때만 적용되는데, 프록시는 진짜 객체와 '별개 객체'다. 진짜 객체 내부의
    this.method()는 프록시가 아니라 진짜 객체 자신을 직접 부르므로 프록시를 우회한다 → 어드바이스 미적용.

- **Q. 그것이 실무에서 어떤 사고로 이어지나?**
  - 내 답: 같은 클래스 안에서 @Transactional 메서드를 내부 호출하면 트랜잭션이 안 걸린다(가장 흔한 함정).
    해결은 클래스 분리/자기 주입/AopContext/AspectJ 전환 — PART 13.3.

- **Q. AspectJ는 왜 내부 호출 한계가 없나?**
  - 내 답: 프록시가 아니라 바이트코드 자체에 어드바이스를 짜 넣어, 거쳐야 할 프록시가 애초에 없기 때문.
    내부/외부 호출 구분 없이 동일하게 가로챈다.

- **Q. 위빙 3종은?**
  - 내 답: 컴파일 타임 위빙(CTW, ajc 컴파일러가 컴파일 때), 로드 타임 위빙(LTW, 클래스 로딩 때 javaagent),
    런타임 프록시(Spring AOP — 엄밀히 위빙 아니라 프록시로 감쌈). 앞 둘은 바이트코드를 고치고, 마지막은 대리 객체를 끼운다.

- **Q. Spring AOP의 한계들(내부 호출 외)과 공통 뿌리는?**
  - 내 답: ①메서드 호출만(생성자·필드·static 불가) ②public만(private 불가) ③내부 호출 불가 ④final 불가.
    공통 뿌리는 "프록시를 거쳐야만 동작" — 프록시가 못 끼는 지점은 전부 미적용. AspectJ는 바이트코드 위빙이라 이 한계가 없다.
