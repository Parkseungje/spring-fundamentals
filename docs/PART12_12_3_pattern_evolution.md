# PART 12 — 프록시의 진화와 Spring AOP: 12.3 디자인 패턴의 진화 — 템플릿 메서드 → 전략 → 템플릿 콜백

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.3 디자인 패턴의 진화**를 다룬다.
> 12.1의 '흩어진 로깅'을 패턴으로 한곳에 모은다. PART 8의 템플릿 메서드·전략 패턴이 재등장하고, 여기에
> '콜백'이 새로 더해진다. 세 패턴 모두 공통 한계(원본 코드 수정)에 부딪히며, 그 한계가 12.4 프록시의 동기가 된다.

---

## 0. 들어가기 전에 — 핵심 용어
- **변하지 않는 부분(템플릿)**: 모든 작업이 공유하는 고정 흐름. 여기선 로그 추적(begin → 비즈니스 → end / 예외 시 exception).
- **변하는 부분**: 작업마다 달라지는 비즈니스 로직(주문 처리, 검증 등).
- **템플릿 메서드 패턴**: 부모 클래스가 흐름을 고정하고, 변하는 부분만 자식이 오버라이드(상속 기반).
- **전략 패턴(Strategy)**: 변하는 부분을 인터페이스(Strategy)로 빼서 객체가 '필드로 들고'(합성) 위임(상속 대신 합성/DI).
- **템플릿 콜백 패턴**: 전략을 '실행 시점에 메서드 파라미터로' 전달. 스프링 전용 용어(Context→Template, Strategy→Callback).
- **콜백(callback)**: "내가 지금 실행하지 않고, 남(템플릿)에게 넘겨주면 그쪽이 나중에 **되불러(call back)** 실행"하는 코드. 여기선 람다.
- **합성(composition, HAS-A)**: 다른 객체를 필드로 '가지고' 일을 위임. ↔ **상속(inheritance, IS-A)**.
- **함수형 인터페이스 / 람다**: 추상 메서드가 1개인 인터페이스(함수형)는 람다 `() -> {...}`로 간결히 구현 가능(PART 5.4). 추상 '클래스'는 람다로 못 만든다.

한 줄 그림: **'변하지 않는 흐름(로깅)'과 '변하는 비즈니스'를 분리하는 방법이 상속(템플릿 메서드) → 합성
(전략) → 실행시점 파라미터(템플릿 콜백)로 진화한다. 그러나 셋 다 결국 호출부를 execute(...)로 감싸야 해서
'원본 코드 수정'을 피하지 못한다. 그 한계를 코드 변경 없이 푸는 게 프록시(12.4).**

---

## 1. 학습 내용

### 1-0. 출발점 — 무엇을 분리하려는가
12.1에서 본 것처럼 로그 추적(begin/end/예외+시간)은 모든 메서드에 똑같이 필요한 '변하지 않는 부가 코드'다.
반면 그 안의 비즈니스(주문/검증)는 '변하는 부분'이다. 목표는 **변하지 않는 흐름을 한곳에 모으고, 변하는
부분만 갈아끼우는 것.** 세 패턴은 "변하는 부분을 어떻게 끼워 넣느냐"가 다를 뿐 목표는 같다.
```
[고정 흐름]  begin --> ( 여기에 비즈니스 ) --> end   (예외 시 exception)
                         ↑ 이 자리를 어떻게 채우느냐 = 패턴의 차이
```

### 1-1. 템플릿 메서드 패턴 — 상속으로 분리
부모 클래스가 흐름(`execute`)을 고정하고, 변하는 부분(`call`)만 추상 메서드로 남겨 자식이 채운다.
```java
abstract class AbstractTraceTemplate {
    public final void execute() {            // 변하지 않는 흐름(템플릿)
        Status s = trace.begin(message);
        try { call(); trace.end(s); }        // call()만 자식이 구현
        catch (Exception e) { trace.exception(s, e); throw e; }
    }
    protected abstract void call();          // 변하는 부분
}
```
- 장점: 로그 보일러플레이트를 부모로 끌어올려 자식엔 비즈니스만 남는다(12.1의 흩어짐 제거).
- **한계 = 상속의 강한 결합**:
  - 작업(주문/검증)마다 부모를 상속한 **클래스(또는 익명 클래스)를 새로 만들어야** 한다 → 작업 수만큼 클래스 증가.
  - 자식이 부모(AbstractTemplate)에 **강하게 묶인다**(부모 변경에 민감, 부모의 protected 규약에 종속).
  - 비즈니스 한 줄을 위해 매번 클래스 껍데기를 써야 한다(번거로움).

> ★ 왜 상속이 '강한 결합'인가 — 자식은 컴파일 시점에 특정 부모를 박아 넣는다. 부모가 바뀌면 모든 자식이
> 영향받고, 자식은 부모 없이 존재할 수 없다. 이 경직성을 푸는 게 '합성'이다(PART 8.4에서 본 흐름과 동일).
>
> ★ 왜 여기선 '익명 클래스'를 쓰고, 전략·콜백에선 '람다'를 쓰나 — 템플릿 메서드의 부모는 '추상 클래스'다.
> 람다 `() -> {...}`는 '추상 메서드 1개짜리 인터페이스(함수형 인터페이스)'만 간결히 구현할 수 있고, 추상
> 클래스는 못 만든다(PART 5.4). 그래서 추상 클래스를 그 자리에서 구현하려면 '익명 클래스'를 써야 한다.
> 반대로 전략(1-2)·콜백(1-3)은 대상이 '인터페이스'라 람다로 간결하게 쓸 수 있다.

### 1-2. 전략 패턴 — 상속 대신 합성(위임)
변하는 부분을 `Strategy` 인터페이스로 빼고, Context가 그것을 **필드로 들고(합성)** 위임한다.
```java
interface BusinessStrategy { void call(); }

class TraceContext {
    private final BusinessStrategy strategy;       // 합성(HAS-A): 비즈니스를 '들고' 있음
    TraceContext(String msg, BusinessStrategy s) { ... }  // 생성자 주입 = DI
    public void execute() {
        Status st = trace.begin(message);
        try { strategy.call(); trace.end(st); }    // 위임
        catch (Exception e) { trace.exception(st, e); throw e; }
    }
}
// 사용: 람다로 비즈니스 전달(익명 클래스 껍데기 불필요)
new TraceContext("order", () -> System.out.println("주문 처리")).execute();
```
- 상속이 사라지고 Context는 **Strategy 인터페이스에만 의존**한다(PART 8.4 DI/DIP와 같은 사상).
- Strategy가 함수형 인터페이스라 **람다**로 간결하게 비즈니스를 전달한다(템플릿 메서드의 익명 클래스 껍데기가 사라짐).

> ★ 전략 패턴은 로깅 전용이 아니다(정통 예시) — "변하는 부분을 인터페이스로 빼서 갈아끼우는" 패턴 자체는
> 어디나 쓰인다. 예: 정렬 기준을 바꾸는 `Comparator`(java-fundamentals PART 5.2), 결제 수단(카드/계좌이체)을
> 갈아끼우기, 할인 정책 교체. 여기선 그 '변하는 부분'이 '비즈니스 로직'일 뿐이다.
- **남는 한계**: 전략을 **Context의 필드로 미리 정해 생성자에 넣는다** → 작업이 바뀌면 Context를 새로 만들어야
  한다(전략이 Context에 고정). 이걸 '실행 시점 전달'로 푸는 게 템플릿 콜백.

> ★ 템플릿 메서드 vs 전략 — 흐름을 가진 쪽이 '부모 클래스'(상속, IS-A)냐 'Context'(합성, HAS-A)냐의 차이.
> 비즈니스가 '자식의 오버라이드'냐 '주입된 객체'냐. 결합도는 상속 > 합성이라, 합성이 더 유연하다.

### 1-3. 템플릿 콜백 패턴 — 전략을 '실행 시점 파라미터'로 (스프링 전용 용어)
전략을 필드가 아니라 **`execute(...)`를 호출하는 그 순간 인자로** 넘긴다. 그러면 템플릿 객체는 **하나만**
두고, 작업마다 다른 콜백을 그때그때 넘기면 된다(작업마다 객체 생성 불필요 → 1-2 한계 해소).
```java
interface Callback<T> { T call() throws Exception; }   // 콜백(나중에 템플릿 안에서 호출됨)

class TraceTemplate {                                   // Context -> Template
    public <T> T execute(String message, Callback<T> cb) {   // 콜백을 '실행 시점에' 받음
        Status s = trace.begin(message);
        try { T r = cb.call(); trace.end(s); return r; }     // 위임
        catch (Exception e) { trace.exception(s, e); throw new RuntimeException(e); }
    }
}
// 사용: 템플릿 '하나'에 작업마다 콜백만 바꿔 전달(값 반환도 가능)
TraceTemplate t = new TraceTemplate();
t.execute("order", () -> { System.out.println("주문 처리"); return null; });
int n = t.execute("validate", () -> { System.out.println("검증 통과"); return 1; });
```
- **용어 매핑(스프링)**: Context → Template, Strategy → Callback. '콜백' = 인수로 넘겨 두었다 나중에 호출되는 실행 코드.
- **왜 중요한가 — 스프링의 `XxxTemplate` 시리즈가 전부 이 패턴**이다:
  - `JdbcTemplate`(10.5): `jdbcTemplate.query("sql", rowMapper)` — 연결/예외/자원 정리(변하지 않는 흐름)는
    템플릿이, '행 → 객체 변환'(변하는 부분)은 `RowMapper` 콜백이 담당.
  - `RestTemplate`, `TransactionTemplate`도 동일 구조. → 12.3을 알면 이 시리즈의 설계가 한 번에 이해된다.

#### ★ 헷갈리는 지점 — '템플릿 메서드 패턴' vs '템플릿 콜백 패턴'
둘 다 '템플릿'이 들어가 같은 것처럼 보이지만 다른 패턴이다.

| | 템플릿 메서드 패턴 | 템플릿 콜백 패턴 |
|---|---|---|
| 출처 | GoF 정통 패턴 | 스프링 전용 용어 |
| 변하는 부분 전달 | 자식이 '상속'해서 오버라이드 | '콜백(인자)'으로 실행 시점에 전달 |
| 기반 | 상속(IS-A) | 합성(HAS-A) + 콜백 |
| 흐름 가진 쪽 | 부모 클래스 | Template 객체(하나 재사용) |

이름은 비슷해도 '상속이냐 / 콜백 주입이냐'가 본질 차이다. 템플릿 콜백 = 전략 패턴을 '실행 시점 인자'로 변형한 스프링식 이름.

> ★ '콜백(callback)'이라는 말의 뜻 — "내가 지금 직접 호출하는 게 아니라, 코드를 남(템플릿)에게 넘겨주면
> 그쪽이 흐름 도중에 **나를 되불러(call back)** 실행"한다는 의미다. 비유: 식당에서 진동벨을 받아 두고(콜백 등록)
> 자리에 앉으면, 음식이 되면 식당이 벨을 울려(되불러) 나를 호출한다. execute(콜백)에 넘긴 람다가 그 진동벨이다.

### 1-4. 세 패턴의 공통 한계 → 프록시(12.4)의 동기
세 패턴 모두 부가 기능을 깔끔히 분리했지만, 결국 **호출부를 `execute(...)`로 감싸도록 비즈니스 코드를
수정해야** 한다. 즉 "원본 코드에 손을 대야 부가 기능이 붙는다".
```
원하는 것: OrderService.order()를 '그대로 두고' 로그를 끼우고 싶다(원본 0 수정).
세 패턴:   결국 order() 호출부를 template.execute(...)로 바꿔야 함 -> 원본 수정 발생.
```
이 마지막 한계(원본 수정)를 **코드 변경 없이** 푸는 것이 **프록시**다(12.4~). 클라이언트와 실제 객체 사이에
대리인을 끼워, 원본을 건드리지 않고 부가 기능을 더한다.

---

## 2. 실습으로 확인하기

> - **가설**: 같은 작업(주문/검증 + 로그)을 세 패턴으로 구현하면, 출력(로그)은 동일하지만 '변하는 부분을
>   끼우는 방식'이 상속 → 합성 → 실행시점 파라미터로 진화하며 결합·번거로움이 줄어든다. 단 셋 다 호출부 수정은 남는다.

### 코드 (`com.study.part12_aop.s03_pattern_evolution`)
- `Trace` — 세 패턴이 공통으로 끼우려는 부가 기능(begin/end/exception).
- `Example1_TemplateMethod` — 부모가 흐름, 자식(익명 클래스)이 비즈니스(상속).
- `Example2_Strategy` — Context가 Strategy를 합성, 람다로 주입(합성/DI).
- `Example3_TemplateCallback` — 템플릿 하나에 콜백을 실행시점 인자로(JdbcTemplate 원리, 값 반환).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s03_pattern_evolution.Example1_TemplateMethod
./gradlew runStage -Pmain=com.study.part12_aop.s03_pattern_evolution.Example2_Strategy
./gradlew runStage -Pmain=com.study.part12_aop.s03_pattern_evolution.Example3_TemplateCallback
```

### 실행 결과 — 가설과 실제 비교 (실측)
세 예제의 로그 출력은 동일하다(부가 기능이 같으니 당연 — 달라진 건 '구조'다):
```
--> [OrderService.order] 시작
    [비즈니스] 노트북 주문 처리
<-- [OrderService.order] 종료 (0ms)
--> [OrderService.validate] 시작
    [비즈니스] 노트북 검증 통과
<-- [OrderService.validate] 종료 (0ms)
```
- Example3는 추가로 콜백의 반환값을 보여준다: `검증 결과 건수 = 1`(JdbcTemplate.query가 결과를 돌려주듯).
- 출력은 같지만 **변하는 부분을 끼우는 방식**이 상속(익명 클래스) → 합성(람다 필드) → 실행시점 콜백으로
  진화했고, Example3는 템플릿 객체 하나를 재사용한다. ✅ 그러나 셋 다 호출부를 execute로 감싼다(원본 수정) → 12.4.

---

## 3. 자기 점검

- **Q. 템플릿 메서드 패턴의 한계는?**
  - 내 답: 상속 기반이라 작업마다 자식 클래스(익명 클래스)가 필요하고, 자식이 부모에 강하게 결합된다.
    비즈니스 한 줄에도 클래스 껍데기가 필요해 번거롭다. → 합성(전략)으로 푼다.

- **Q. 전략 패턴은 그 한계를 어떻게 푸나? 남는 한계는?**
  - 내 답: 상속 대신 합성으로, Context가 Strategy 인터페이스를 필드로 들고 위임한다(DI, 람다로 간결).
    그러나 전략이 Context의 필드로 고정돼 작업마다 Context를 새로 만들어야 한다.

- **Q. 템플릿 콜백 패턴이란? 스프링 어디에 쓰이나?**
  - 내 답: 전략을 필드가 아니라 execute(...) 호출 시점에 파라미터로 넘기는 것(Context→Template,
    Strategy→Callback). 템플릿 하나로 작업마다 콜백만 바꿔 재사용한다. JdbcTemplate(RowMapper 콜백)·
    RestTemplate·TransactionTemplate 등 XxxTemplate 시리즈가 모두 이 패턴.

- **Q. '템플릿 메서드 패턴'과 '템플릿 콜백 패턴'은 같은 건가?**
  - 내 답: 다르다. 템플릿 메서드는 GoF 정통 패턴으로 '상속'(자식이 오버라이드) 기반, 템플릿 콜백은 스프링
    용어로 '콜백(인자)'을 실행 시점에 넘기는 합성 기반. 이름만 비슷하고 본질은 상속이냐/콜백 주입이냐로 다르다.

- **Q. 템플릿 메서드는 익명 클래스, 전략·콜백은 람다를 쓰는 이유는?**
  - 내 답: 템플릿 메서드의 대상은 추상 '클래스'라 람다로 못 만들어 익명 클래스를 쓴다. 전략·콜백은 대상이
    함수형 '인터페이스'(추상 메서드 1개)라 람다로 간결히 구현된다(PART 5.4).

- **Q. 세 패턴의 공통 한계와, 그것이 무엇으로 이어지나?**
  - 내 답: 셋 다 부가 기능을 붙이려면 호출부를 execute(...)로 감싸도록 원본 코드를 수정해야 한다. 이
    '원본 수정' 한계를 코드 변경 없이 푸는 것이 프록시(12.4)다 — 클라이언트와 실제 객체 사이 대리인을 끼운다.
