package com.study.part12_aop.s04_proxy;

/**
 * [12.4] '부가 기능' 프록시 — 로그/시간 측정을 더한다(접근은 그대로 통과시킴).
 *
 * 프록시의 두 기능 중 '부가 기능(로그·시간 측정)'에 해당한다. 진짜 객체와 '같은 인터페이스'(OrderService)를
 * 구현하고, 내부에 진짜 객체(target)를 들고 있다가, 호출이 오면 begin/end로 감싼 뒤 그대로 target에 위임한다.
 *  - 핵심: 호출을 '막지 않는다'. 항상 진짜 객체를 부른다(=접근 제어가 아님). 단지 앞뒤로 부가 코드를 더할 뿐.
 *  - RealOrderService를 한 줄도 수정하지 않고 로그가 붙는다 -> 12.3의 '원본 수정' 한계 극복.
 *
 * (이 패턴은 '의도상' 데코레이터에 가깝다 - 12.5에서 프록시 vs 데코레이터의 의도 차이를 다룬다.)
 */
public class LogProxy implements OrderService {

    private final OrderService target; // 진짜 객체(또는 또 다른 프록시) — 같은 인터페이스 타입
    private final Trace trace = new Trace();

    public LogProxy(OrderService target) {
        this.target = target;
    }

    @Override
    public String order(String item) {
        Trace.Status status = trace.begin("OrderService.order");
        try {
            String result = target.order(item); // 부가 코드 사이에서 '그대로 위임'(접근 막지 않음)
            trace.end(status);
            return result;
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }

    @Override
    public int findStock(String item) {
        Trace.Status status = trace.begin("OrderService.findStock");
        try {
            int result = target.findStock(item); // 마찬가지로 그대로 위임
            trace.end(status);
            return result;
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }
}
