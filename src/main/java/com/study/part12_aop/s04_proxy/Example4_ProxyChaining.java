package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 예제4 — 프록시 체이닝: 프록시의 target 자리에 '또 다른 프록시'를 넣어 기능을 겹친다.
 *
 * 프록시는 '진짜 객체'만 감싸는 게 아니다. target 타입이 인터페이스(OrderService)이므로, 그 자리에 또
 * 다른 프록시를 넣을 수 있다. 그러면 호출이 프록시 -> 프록시 -> ... -> 진짜 객체로 줄줄이 흐른다(체이닝).
 * 이렇게 부가 기능(로그)과 접근 제어(캐싱)를 '동시에' 적용할 수 있다.
 *
 * 구성: LogProxy(바깥) -> CachingProxy(안) -> RealOrderService(진짜)
 *   - findStock 첫 호출: Log 시작 -> Caching(미스, 진짜 조회) -> Log 종료
 *   - findStock 둘째 호출: Log 시작 -> Caching(히트, 진짜 호출 생략) -> Log 종료
 *     => 로그는 두 번 다 찍히지만(바깥 LogProxy), 진짜 조회는 한 번만(안쪽 CachingProxy가 막음).
 *
 * Example2(LogProxy만)·Example3(CachingProxy만)와 달리, 둘을 '겹쳐' 로그+캐싱을 함께 얻는다. 감싸는
 * 순서를 바꾸면(Caching 바깥/Log 안) 동작 순서도 달라진다 — 바깥 프록시가 먼저 가로챈다.
 * (Client/Real 코드는 여전히 0줄 수정. 주입할 때 감싸는 조립만 바꾼다.)
 */
public class Example4_ProxyChaining {

    public static void main(String[] args) {
        System.out.println("== [프록시 체이닝] LogProxy -> CachingProxy -> Real ==");

        OrderService real = new RealOrderService();
        OrderService chain = new LogProxy(new CachingProxy(real)); // 프록시가 프록시를 감쌈
        Client client = new Client(chain);
        client.run();   // order 1회 + findStock 2회

        System.out.println("\n=> 바깥 LogProxy가 매번 로그를 찍고, 안쪽 CachingProxy가 두 번째 findStock의 진짜 조회를");
        System.out.println("   막는다(로그 2번 / 진짜 조회 1번). 프록시를 겹쳐 로그+캐싱을 동시에 적용했다.");
        System.out.println("   (Client/Real은 그대로 — 주입 시 감싸는 조립만 바꿨다. 실무 여러 부가기능 적용의 토대.)");
    }
}
