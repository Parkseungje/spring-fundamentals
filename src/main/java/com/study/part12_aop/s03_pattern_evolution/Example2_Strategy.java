package com.study.part12_aop.s03_pattern_evolution;

/**
 * [12.3] 예제2 — 전략 패턴: 상속 대신 '합성(위임)'. Context는 Strategy 인터페이스에만 의존(= DI).
 *
 * 예제1의 한계는 '상속'이었다(작업마다 자식 클래스, 부모에 강결합). 전략 패턴은 변하는 부분(비즈니스)을
 * 'Strategy'라는 인터페이스로 빼고, Context가 그 인터페이스를 '필드로 들고(합성)' 위임 호출한다.
 *  - 상속이 아니라 합성 -> 부모-자식 결합이 사라지고, Context는 Strategy 인터페이스에만 의존(PART 8.4 DI/DIP).
 *  - Strategy가 함수형 인터페이스이므로 '람다'로 비즈니스를 간결하게 전달할 수 있다(익명 클래스 껍데기 불필요).
 *
 * 예제1과의 차이: 흐름(begin/end/예외)을 가진 쪽이 '부모 클래스'가 아니라 'Context'이고, 비즈니스는
 * '자식의 오버라이드'가 아니라 '주입된 Strategy'다. 즉 IS-A(상속)에서 HAS-A(합성)로 바뀌었다.
 *
 * 남는 한계(예제3로 이어짐): Strategy를 'Context의 필드'로 미리 정해 생성자에 넣는다 -> 작업이 바뀌면
 * Context를 새로 만들어야 한다(전략이 Context에 고정됨). 이걸 '실행 시점 파라미터'로 푸는 게 템플릿 콜백.
 */
public class Example2_Strategy {

    // 변하는 부분을 담는 전략 인터페이스(함수형 -> 람다 가능).
    interface BusinessStrategy {
        void call();
    }

    // 변하지 않는 흐름을 가진 Context. Strategy를 '필드로 합성'해 위임한다(상속 아님).
    static class TraceContext {
        private final Trace trace = new Trace();
        private final String message;
        private final BusinessStrategy strategy; // 합성: 비즈니스를 '들고' 있다(생성자 주입 = DI)

        TraceContext(String message, BusinessStrategy strategy) {
            this.message = message;
            this.strategy = strategy;
        }

        public void execute() {
            Trace.Status status = trace.begin(message);
            try {
                strategy.call();     // 위임: 주입된 전략에 비즈니스를 맡긴다
                trace.end(status);
            } catch (Exception e) {
                trace.exception(status, e);
                throw e;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("== [전략 패턴] 합성 + 람다로 비즈니스 주입 ==");

        // 비즈니스를 '람다'로 전달(익명 클래스 껍데기 없이 간결). 단 전략은 Context 생성 시 고정된다.
        TraceContext order = new TraceContext("OrderService.order",
                () -> System.out.println("    [비즈니스] 노트북 주문 처리"));
        TraceContext validate = new TraceContext("OrderService.validate",
                () -> System.out.println("    [비즈니스] 노트북 검증 통과"));

        order.execute();
        validate.execute();

        System.out.println("\n=> 상속이 사라지고 Context는 Strategy 인터페이스에만 의존(DI). 람다로 간결해졌다.");
        System.out.println("   그러나 전략이 'Context의 필드'로 고정 -> 작업마다 Context를 새로 만든다. 예제3이 이를 푼다.");
    }
}
