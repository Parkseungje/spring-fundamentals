package com.study.part12_aop.s03_pattern_evolution;

/**
 * [12.3] 예제1 — 템플릿 메서드 패턴: 변하지 않는 흐름은 부모가, 변하는 부분만 자식이 구현.
 *
 * 아이디어: begin -> (비즈니스) -> end / 예외 시 exception 이라는 '흐름(템플릿)'을 부모 클래스가 고정하고,
 * 진짜 달라지는 '비즈니스 로직(call)'만 자식이 채운다. 그러면 로그 보일러플레이트(try-catch+begin/end)를
 * 자식마다 반복하지 않아도 된다 -> 12.1의 흩어짐을 부모로 끌어올려 제거.
 *
 * 한계(이 예제가 동시에 보여주려는 것): '상속'을 쓴다는 점이다.
 *  - 작업(주문/검증)마다 부모를 상속한 클래스(또는 익명 클래스)를 만들어야 한다 -> 작업 수만큼 클래스 증가.
 *  - 자식이 부모(AbstractTemplate)에 강하게 결합된다(부모 변경에 민감, 부모의 protected 규약에 묶임).
 *  - 비즈니스 로직 한 줄을 위해 매번 클래스 껍데기를 쓰는 게 번거롭다.
 * 이 '상속의 강한 결합'을 합성으로 푸는 게 예제2(전략 패턴)다.
 */
public class Example1_TemplateMethod {

    // 변하지 않는 흐름을 가진 '템플릿'. execute()가 흐름을 고정하고, call()만 자식에게 맡긴다.
    abstract static class AbstractTraceTemplate {
        private final Trace trace = new Trace();
        private final String message;

        AbstractTraceTemplate(String message) {
            this.message = message;
        }

        // 템플릿 메서드: 이 흐름(begin/try-end/catch-exception)은 모든 작업이 공유하며 바뀌지 않는다.
        public final void execute() {
            Trace.Status status = trace.begin(message);
            try {
                call();              // 변하는 부분 = 자식이 구현한 비즈니스 로직
                trace.end(status);
            } catch (Exception e) {
                trace.exception(status, e);
                throw e;
            }
        }

        // 자식이 반드시 채워야 하는 '변하는 부분'.
        protected abstract void call();
    }

    public static void main(String[] args) {
        System.out.println("== [템플릿 메서드] 흐름은 부모, 비즈니스만 자식(익명 클래스) ==");

        // 작업마다 '익명 클래스'로 call()을 구현해야 한다(상속). 비즈니스는 한 줄인데 껍데기가 크다.
        AbstractTraceTemplate order = new AbstractTraceTemplate("OrderService.order") {
            @Override
            protected void call() {
                System.out.println("    [비즈니스] 노트북 주문 처리");
            }
        };
        AbstractTraceTemplate validate = new AbstractTraceTemplate("OrderService.validate") {
            @Override
            protected void call() {
                System.out.println("    [비즈니스] 노트북 검증 통과");
            }
        };

        order.execute();
        validate.execute();

        System.out.println("\n=> 로그 흐름(begin/end/예외)을 부모가 전담해 자식엔 비즈니스만 남았다.");
        System.out.println("   그러나 작업마다 상속(익명 클래스)이 필요하고 부모에 강하게 묶인다 -> 예제2가 합성으로 푼다.");
    }
}
