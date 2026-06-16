# PART 12 — 프록시의 진화와 Spring AOP: 12.5 프록시 패턴 vs 데코레이터 패턴 (의도로 구분)

> 이 문서는 커리큘럼 PART 12의 소단원 중 **12.5 프록시 패턴 vs 데코레이터 패턴**을 다룬다.
> 12.4에서 프록시의 두 기능(부가 기능/접근 제어)을 봤다. 그런데 GoF 디자인 패턴에서는 '데코레이터 패턴'과
> '프록시 패턴'을 따로 부른다. 둘은 코드 모양이 거의 같아 헷갈린다. 무엇으로 구분하는가? '의도'로 구분한다.

---

## 0. 들어가기 전에 — 핵심 용어
- **데코레이터 패턴(Decorator)**: 대상을 감싸 **기능을 추가**하는 패턴(로깅·꾸미기·시간 측정·변환·버퍼링).
- **프록시 패턴(Proxy)**: 대상을 감싸 **접근을 제어**하는 패턴(권한 검사·캐싱·지연 로딩).
- **위임(delegate)**: 감싼 객체가 받은 호출을 안쪽 대상에게 넘기는 것.
- **쌓기(stacking)**: 감싼 객체를 또 감싸 기능을 누적하는 것. 데코레이터의 전형적 사용법.
- **GoF 디자인 패턴**: 객체지향 설계의 고전적 패턴 모음. 프록시·데코레이터를 별개 패턴으로 정의한다.

한 줄 그림: **데코레이터와 프록시는 '구조(같은 인터페이스 구현 + 대상 위임)'가 사실상 같다. 다른 건 '의도'
뿐이다 — 데코레이터는 "기능을 더하려고"(항상 위임 후 결과 가공·쌓기), 프록시는 "접근을 통제하려고"(호출을
허용/차단·캐싱·지연). 그래서 모양이 아니라 '왜 감쌌는가'로 구분한다.**

---

## 1. 학습 내용

### 1-1. 둘은 '구조'가 같다 — 그래서 헷갈린다
데코레이터든 프록시든 코드 골격은 동일하다.
```
1) 대상과 '같은 인터페이스'를 구현한다(TextSource)
2) 대상(target)을 필드로 들고 있다
3) 호출을 가로채(read()) 무언가 한 뒤 대상에 위임한다
```
12.4에서 이미 이 골격을 봤다(LogProxy/CachingProxy). 구조만 보면 둘을 구분할 수 없다. 그래서 GoF는 구조가
아니라 **'의도(intent)'로 이름을 나눈다.**

### 1-2. 데코레이터 패턴 — '기능 추가'가 의도
대상에 **항상 위임**한 뒤, 그 결과에 기능을 더하거나 동작을 꾸민다. 접근을 막지 않는다.
```java
class UpperCaseDecorator implements TextSource {
    private final TextSource target;
    public String read() {
        String result = target.read();        // 항상 위임
        return result.toUpperCase();          // 결과에 기능 추가
    }
}
```
- 특징: **쌓을 수 있다.** target 자리에 또 다른 데코레이터를 넣으면 기능이 누적된다.
  ```
  new BracketDecorator(new UpperCaseDecorator(plain))   // 대문자화 -> 괄호  : hello -> HELLO -> [HELLO]
  ```
- 예: 로깅·시간 측정·변환·버퍼링. 12.4의 LogProxy도 '의도상' 데코레이터에 가깝다(접근을 막지 않고 로그만 더함).

### 1-3. 프록시 패턴 — '접근 제어'가 의도
대상으로의 **접근 여부 자체를 통제**한다. 권한이 없으면 대상을 아예 안 부르고 차단하거나, 캐시가 있으면
대상을 생략하거나, 처음 쓸 때만 대상을 생성한다(지연 로딩).
```java
class AccessControlProxy implements TextSource {
    private final TextSource target;
    private final boolean authorized;
    public String read() {
        if (!authorized) throw new SecurityException("권한 없음"); // 대상 호출 차단(접근 제어)
        return target.read();                                      // 권한 있을 때만 위임(결과 가공 안 함)
    }
}
```
- 특징: 결과를 '꾸미지' 않는다. 핵심은 "통과시킬까 말까/언제 통과시킬까"의 결정이다.
- 예: 권한 검사(보호 프록시), 캐싱, 지연 로딩(가상 프록시), 원격 객체 대리(원격 프록시).

### 1-4. 구분 기준 — '왜 감쌌는가'
| | 데코레이터 패턴 | 프록시 패턴 |
|---|---|---|
| 의도 | 기능 추가(꾸미기) | 접근 제어 |
| 대상 호출 | 항상 위임 | 허용/차단/생략을 통제 |
| 결과 | 가공한다(변환·꾸밈) | 보통 그대로(가공 안 함) |
| 쌓기 | 여러 겹 조합이 자연스러움 | 보통 단일(접근 통제) |
| 예 | 로깅·시간측정·변환, 자바 I/O 스트림 | 권한·캐싱·지연 로딩·원격 |

핵심: **모양이 아니라 의도로 구분.** "결과를 더 풍부하게 하려고 감쌌나(데코레이터)" vs "접근을 통제하려고
감쌌나(프록시)". 그래서 같은 코드라도 쓰임의 목적에 따라 이름이 달라질 수 있다.

> ★ 헷갈리는 점: "그럼 LogProxy는 프록시야 데코레이터야?" — 이름엔 Proxy가 붙었지만 '로그 추가'라는
> 의도만 보면 데코레이터에 가깝다. 실무·스프링에서는 이 둘을 엄격히 가르기보다 '프록시'로 뭉뚱그려 부르는
> 경우가 많다(Spring AOP의 프록시는 사실상 기능 추가=데코레이터적 용도가 많다). 시험·면접에서는 "구조는
> 같고 의도로 구분한다"는 점이 정답이다.

### 1-5. 자바 I/O = 데코레이터의 대표 사례 (PART 6과 연결)
이 패턴은 학습용 장난감이 아니라 JDK 표준에 박혀 있다. 자바 I/O 스트림 래핑(PART 6)이 데코레이터다.
```
InputStream(원본 바이트)
  └ InputStreamReader(바이트 -> 문자 '변환' 기능 추가)
      └ BufferedReader(버퍼링 + readLine() 기능 추가)
```
- 각 래퍼는 안쪽 스트림에 위임하면서 기능을 더하고, '쌓아서' 조합한다(1-2와 똑같은 구조).
- 의도가 '기능 추가'(바이트→문자 변환, 버퍼링, 줄 단위 읽기)라 데코레이터로 분류된다(접근 제어가 아님).
- → PART 6에서 "왜 `new BufferedReader(new InputStreamReader(...))`처럼 겹겹이 감싸나"가 여기서 패턴으로 설명된다.

---

## 2. 실습으로 확인하기

> - **가설**: 같은 TextSource 인터페이스로 데코레이터(기능 추가·쌓기)와 프록시(접근 제어·차단)를 만들면,
>   코드 구조는 같지만 동작의 의도가 다르다. 그리고 자바 I/O가 실제 데코레이터임을 확인한다.

### 코드 (`com.study.part12_aop.s05_proxy_vs_decorator`)
- `TextSource`(인터페이스), `PlainTextSource`(원본).
- `UpperCaseDecorator`/`BracketDecorator`(데코레이터, 기능 추가·쌓기), `AccessControlProxy`(프록시, 접근 제어).
- `Example1_DecoratorPattern` / `Example2_ProxyPattern` / `Example3_JavaIoDecorator`.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part12_aop.s05_proxy_vs_decorator.Example1_DecoratorPattern
./gradlew runStage -Pmain=com.study.part12_aop.s05_proxy_vs_decorator.Example2_ProxyPattern
./gradlew runStage -Pmain=com.study.part12_aop.s05_proxy_vs_decorator.Example3_JavaIoDecorator
```

### 실행 결과 — 가설과 실제 비교 (실측)
예제1 (데코레이터, 쌓기):
```
    [원본] read() 호출 -> "hello" 반환
    [데코:대문자] "hello" -> "HELLO"
    [데코:괄호] "HELLO" -> "[HELLO]"
  최종 결과 = [HELLO]
```
- 원본 → 대문자 → 괄호로 기능이 누적됐다. 항상 위임 + 결과 가공 + 쌓기. ✅

예제2 (프록시, 접근 제어):
```
[권한 있음]  [프록시:접근제어] 권한 확인됨 -> 위임   -> 결과 = secret-data
[권한 없음]  [프록시:접근제어] 권한 없음 -> 진짜 객체 호출 차단!   -> SecurityException
```
- 권한 있으면 위임(결과 그대로), 없으면 진짜 호출을 막고 차단. 결과를 꾸미지 않음. ✅ → 구조는 데코레이터와
  같지만 '접근 제어'라는 의도가 다르다.

예제3 (자바 I/O 데코레이터):
```
  1번째 줄: first line
  2번째 줄: second line
```
- `InputStream → InputStreamReader → BufferedReader`로 '쌓아' 감싸 바이트→문자→줄 단위 기능을 누적. ✅ →
  Example1과 똑같은 구조이며 의도가 '기능 추가'라 데코레이터. (PART 6 I/O가 곧 데코레이터)

---

## 3. 자기 점검

- **Q. 프록시 패턴과 데코레이터 패턴은 무엇으로 구분하나?**
  - 내 답: 구조(같은 인터페이스 + 대상 위임)는 사실상 같고, '의도'로 구분한다. 데코레이터는 기능 추가
    (항상 위임 후 결과 가공, 쌓기), 프록시는 접근 제어(호출 허용/차단/생략).

- **Q. 데코레이터의 대표 특징과 예는?**
  - 내 답: 대상에 항상 위임하고 결과를 가공하며, 여러 겹 쌓아 기능을 누적한다. 자바 I/O 스트림 래핑
    (InputStreamReader, BufferedReader)이 대표 사례(PART 6).

- **Q. 프록시의 대표 특징과 예는?**
  - 내 답: 대상으로의 접근 여부를 통제한다. 권한 없으면 차단, 캐시 있으면 생략, 처음 쓸 때만 생성(지연
    로딩). 결과를 꾸미지 않는 게 보통.

- **Q. LogProxy는 프록시인가 데코레이터인가?**
  - 내 답: 의도(로그 추가=기능 추가)로 보면 데코레이터에 가깝다. 다만 실무/스프링에선 둘을 엄격히 가르기보다
    '프록시'로 뭉뚱그려 부르며, Spring AOP의 프록시도 기능 추가 용도가 많다. 정답은 "구조는 같고 의도로 구분".
