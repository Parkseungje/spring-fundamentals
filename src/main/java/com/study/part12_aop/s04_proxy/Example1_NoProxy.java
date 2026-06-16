package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 예제1 — 프록시 없음(기준선). 클라이언트가 진짜 객체를 직접 주입받는다.
 *
 * 가설: 프록시가 없으면 부가 기능(로그)도 없고, 재고 조회는 매번 '실제로' 수행되어 느리다(캐시 없음).
 * 이 예제는 예제2/3과 비교할 '원본 그대로'의 모습을 보여준다. 주목할 점: 여기서 주입을 LogProxy/CachingProxy로
 * 바꿔도 Client 코드는 한 줄도 안 바뀐다(예제2/3에서 확인) -- 클라이언트가 인터페이스에만 의존하기 때문.
 */
public class Example1_NoProxy {

    public static void main(String[] args) {
        System.out.println("== [기준선] 프록시 없음: 진짜 객체를 직접 주입 ==");

        OrderService real = new RealOrderService();
        Client client = new Client(real); // 진짜 객체 주입
        client.run();

        System.out.println("\n=> 로그 없음(부가 기능 0). 재고 조회는 두 번 다 '실제로' 수행(캐시 없음 -> 둘 다 느림).");
        System.out.println("   이 상태에 원본/클라이언트 수정 없이 로그·캐싱을 더하는 게 프록시다(예제2, 예제3).");
    }
}
