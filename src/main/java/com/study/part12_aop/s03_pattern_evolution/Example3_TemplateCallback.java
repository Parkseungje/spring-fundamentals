package com.study.part12_aop.s03_pattern_evolution;

/**
 * [12.3] 예제3 — 템플릿 콜백 패턴(스프링 전용 용어): 전략을 '실행 시점에 파라미터'로 전달.
 *
 * 전략 패턴(예제2)은 전략을 'Context의 필드'로 미리 주입했다. 템플릿 콜백은 한 발 더 나가, 전략을
 * 'execute(...)를 호출하는 그 순간 인자로' 넘긴다. 그래서 템플릿(Context 역할) 객체는 '하나만' 두고,
 * 작업마다 다른 콜백을 그때그때 넘기면 된다 -> 작업마다 객체를 새로 만들 필요가 없다(예제2의 한계 해소).
 *
 * 용어 매핑(스프링): Context -> Template, Strategy -> Callback.
 *  - 콜백(callback) = "인수로 넘겨 두었다가 나중에(템플릿 안에서) 호출되는 실행 코드". 여기선 람다.
 *
 * 왜 중요한가: Spring의 JdbcTemplate(10.5)·RestTemplate·TransactionTemplate 같은 'XxxTemplate' 시리즈가
 * 전부 이 패턴이다. 예: jdbcTemplate.query("sql", rowMapper) 에서 RowMapper가 바로 콜백 -- 연결/예외/자원
 * 정리라는 '변하지 않는 흐름'은 템플릿이, '행->객체 변환'이라는 변하는 부분만 콜백이 담당한다.
 *
 * 공통 한계(세 패턴 모두): 결국 비즈니스 코드(호출부)를 'execute(...)로 감싸도록 수정'해야 한다. 즉 원본
 * 코드에 손을 대야 부가 기능이 붙는다. 이 마지막 한계를 코드 수정 없이 푸는 것이 '프록시'다(12.4~).
 */
public class Example3_TemplateCallback {

    // 콜백 인터페이스 = "변하는 부분(비즈니스 로직)을 담아 템플릿에 넘기는 상자".
    //   템플릿은 begin -> (여기서 callback.call() 실행) -> end 흐름을 고정하고, 그 '여기' 자리에 끼울 코드를
    //   이 Callback으로 받는다. 호출자가 람다 () -> {...} 를 넘기면 그게 곧 이 Callback의 구현이고,
    //   템플릿이 흐름 도중에 그 람다를 '되불러(call back)' 실행한다(= 콜백).
    //
    //   <T> = "call()이 돌려줄 값의 타입"을 가리키는 빈칸(제네릭 타입 파라미터). 클래스에 고정된 게 아니라,
    //   execute를 '호출할 때마다' 그때 넘긴 람다의 return 값을 보고 컴파일러가 채운다(타입 추론). 그래서 같은
    //   템플릿 하나로 반환값 없는 작업(T=Object)·Integer 반환 작업(T=Integer)·String 반환 작업을 다 처리한다.
    interface Callback<T> {
        T call() throws Exception;
    }

    // 템플릿: 단 하나만 만들어 재사용한다. 흐름(begin/end/예외)을 고정하고, 변하는 부분은 '인자로' 받는다.
    static class TraceTemplate {
        private final Trace trace = new Trace();

        // 맨 앞 <T> = "이 메서드는 T라는 타입 변수를 쓴다"는 선언. 인자 Callback<T>와 반환 T가 같은 T다.
        // T가 무엇이 될지는 '호출 시점에' 넘긴 콜백(람다)의 반환값으로 정해진다(아래 main 참고).
        public <T> T execute(String message, Callback<T> callback) {  // 콜백을 '실행 시점에' 받는다
            Trace.Status status = trace.begin(message);
            try {
                T result = callback.call();   // 위임: 그때그때 넘어온 콜백 실행
                trace.end(status);
                return result;
            } catch (Exception e) {
                trace.exception(status, e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("== [템플릿 콜백] 템플릿 하나 + 작업마다 콜백을 인자로 ==");

        TraceTemplate template = new TraceTemplate();  // 템플릿은 '하나'만(예제2는 작업마다 Context를 새로 만듦)

        // 작업마다 콜백(람다)만 바꿔 같은 템플릿에 넘긴다. 객체 추가 생성 없음.
        // 이 람다는 return null -> 반환을 안 쓰므로 T는 의미 없음(컴파일러가 Object로 추론).
        template.execute("OrderService.order", () -> {
            System.out.println("    [비즈니스] 노트북 주문 처리");
            return null;
        });
        // 이 람다는 return 1(Integer) -> 컴파일러가 T=Integer로 추론 -> execute 반환 타입도 Integer ->
        // int count로 받을 수 있다(언박싱). 즉 <T>는 '이 호출'에서 람다 반환값을 보고 Integer로 정해진다.
        int count = template.execute("OrderService.validate", () -> {
            System.out.println("    [비즈니스] 노트북 검증 통과");
            return 1; // 검증 통과 건수 등 (JdbcTemplate.query가 결과를 돌려주듯)
        });
        System.out.println("    검증 결과 건수 = " + count);

        System.out.println("\n=> 템플릿 하나를 재사용하고 작업마다 콜백만 인자로 전달(JdbcTemplate/RestTemplate의 원리).");
        System.out.println("   ★ 그러나 세 패턴 모두 호출부를 execute(...)로 '감싸도록 수정'해야 한다 -> 원본 수정.");
        System.out.println("   이 마지막 한계(원본 수정)를 코드 변경 없이 푸는 것이 '프록시'다(12.4).");
    }
}
