package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 진짜 객체(real subject) — 순수 비즈니스 로직만 가진다. 로그·캐싱 같은 부가 코드는 '전혀 없다'.
 *
 * 12.3의 한계는 부가 기능을 붙이려면 이 비즈니스 코드를 execute(...)로 감싸도록 '수정'해야 한다는 것이었다.
 * 프록시의 목표는 이 클래스를 '한 줄도 건드리지 않고' 로그·캐싱을 더하는 것이다. 그래서 여기엔 의도적으로
 * 부가 코드를 하나도 두지 않는다(순수 핵심 관심사). 부가 기능은 프록시가 바깥에서 더한다.
 */
public class RealOrderService implements OrderService {

    @Override
    public String order(String item) {
        System.out.println("    [비즈니스] " + item + " 주문 처리");
        return item + " 주문완료";
    }

    @Override
    public int findStock(String item) {
        // 비싼 조회를 흉내 낸다(DB·외부 API 호출이라 가정). 캐싱 프록시의 효과를 드러내기 위함.
        System.out.println("    [비즈니스] " + item + " 재고를 '실제로' 조회(느림)...");
        sleep(200);
        return 10; // 재고 수량(가정)
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
