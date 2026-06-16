package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 예제3 — '접근 제어' 프록시(캐싱) 적용. 진짜 호출 자체를 줄인다.
 *
 * 가설: CachingProxy로 감싸면 첫 findStock은 진짜 객체로 내려가 느리지만(200ms), 두 번째 같은 인자 호출은
 * 캐시에서 즉시 반환되어 '진짜 조회가 생략'된다. 이것이 LogProxy(부가 기능)와 다른 '접근 제어'다 -- 단순히
 * 앞뒤를 꾸미는 게 아니라 진짜 객체로의 '접근 여부'를 프록시가 결정한다.
 *
 * 예제2와의 차이: 주입 객체가 LogProxy -> CachingProxy로 바뀐 것뿐인데(Client 동일), 두 번째 조회의 동작이
 * 완전히 달라진다(진짜 호출 vs 캐시 반환). 같은 프록시 모양, 다른 '의도'(접근 제어) -> 12.5 복선.
 */
public class Example3_CachingProxy {

    public static void main(String[] args) {
        System.out.println("== [접근 제어 프록시] CachingProxy로 재고 조회 캐싱 ==");

        OrderService real = new RealOrderService();
        OrderService proxy = new CachingProxy(real);
        Client client = new Client(proxy);
        client.run();

        System.out.println("\n=> 첫 findStock은 '실제 조회(느림)', 두 번째는 '캐시에서 즉시'(진짜 호출 생략).");
        System.out.println("   LogProxy(항상 위임)와 달리 CachingProxy는 진짜 객체 접근을 '제어'한다 = 접근 제어 기능.");
        System.out.println("   Client/Real은 예제1·2와 동일 -- 주입만 바꿔 동작을 바꿨다(프록시+DI의 힘).");
    }
}
