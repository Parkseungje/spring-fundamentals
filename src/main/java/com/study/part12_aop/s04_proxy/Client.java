package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 클라이언트 — 프록시 성립 조건 2: '인터페이스에만 의존 + DI로 주입'.
 *
 * 이 클라이언트는 OrderService '인터페이스'만 알고, 생성자로 주입받은 게 진짜 객체인지 프록시인지 모른다
 * (= 알 필요가 없다). 그래서 main에서 무엇을 주입하느냐(Real / LogProxy / CachingProxy)에 따라 동작이
 * 달라지지만, 이 클래스의 코드는 '한 줄도' 바뀌지 않는다. 이것이 "원본·클라이언트 수정 0으로 부가 기능을
 * 끼운다"는 프록시의 핵심이며, 12.3 세 패턴이 끝내 풀지 못한 '원본 수정' 한계를 넘는 지점이다.
 */
public class Client {

    private final OrderService orderService; // 구체 클래스가 아니라 인터페이스에 의존(DIP)

    public Client(OrderService orderService) { // 무엇이 들어올지 모른 채 주입받음(DI)
        this.orderService = orderService;
    }

    public void run() {
        String result = orderService.order("노트북");
        System.out.println("    클라이언트가 받은 결과 = " + result);

        int stock1 = orderService.findStock("노트북");
        int stock2 = orderService.findStock("노트북"); // 같은 조회를 한 번 더(캐싱 프록시면 두 번째가 빨라야 함)
        System.out.println("    재고 조회 결과 = " + stock1 + ", " + stock2);
    }
}
