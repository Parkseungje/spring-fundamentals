# PART 8 — 객체 설계의 진화 → IoC/DI

## 코드 대비
| | 패키지 | 핵심 |
|---|---|---|
| 로우레벨(고통) | `part08_ioc.traditional` | `MessagePrinter`가 `new ConsoleWriter()`로 직접 생성·결정 (OCP 위반) |
| 하이레벨(해결) | `part08_ioc.improved` | `MessagePrinter`는 `MessageWriter` 인터페이스에만 의존, 구현체 선택은 Spring 컨테이너(IoC)가 담당 |

## 확인할 것
1. `ApplicationContext`에서 `MessagePrinter` 빈을 꺼내 `getBean()`을 여러 번 호출해도
   **같은 인스턴스**가 반환되는지 확인 (싱글톤 레지스트리, 8.6).
   ```java
   var ctx = SpringApplication.run(StudyApplication.class);
   var p1 = ctx.getBean(MessagePrinter.class);
   var p2 = ctx.getBean(MessagePrinter.class);
   System.out.println(p1 == p2); // true
   ```
2. `--debug` 옵션으로 부팅 시 컴포넌트 스캔/빈 등록 로그를 확인
   ```bash
   ./gradlew bootRun --args='--debug'
   ```
3. (PART 12 예고) `MessagePrinter`에 `@Transactional`이나 `@Around` AOP를 붙이면
   실제 등록되는 빈이 프록시 객체로 바뀌는지 `p1.getClass()`로 확인해본다.

## 자기 점검
- DIP와 DI(Dependency Injection)의 관계는? → (직접 정리)
- 생성자 주입을 권장하는 3가지 이유는? → (직접 정리, `MessagePrinter`의 `final` 필드와 연결)
