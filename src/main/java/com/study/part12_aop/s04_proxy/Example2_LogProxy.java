package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 예제2 — '부가 기능' 프록시(로그) 적용. 원본·클라이언트 수정 0.
 *
 * 가설: 진짜 객체를 LogProxy로 감싸 클라이언트에 주입하면, 클라이언트는 '진짜인 줄 알고' 호출하지만
 * 실제로는 프록시가 가로채 begin/end 로그를 더한 뒤 진짜 객체에 위임한다. RealOrderService도 Client도
 * 한 줄 안 바뀐다(예제1과 Client 코드 동일) -- 12.3이 못 푼 '원본 수정' 한계를 넘는다.
 *
 * 예제1과의 차이: 오직 main에서 주입하는 객체가 Real -> LogProxy(Real)로 바뀐 것뿐이다.
 */
public class Example2_LogProxy {

    public static void main(String[] args) {
        System.out.println("== [부가 기능 프록시] LogProxy로 감싸 로그 추가(원본 수정 0) ==");

        OrderService real = new RealOrderService();
        OrderService proxy = new LogProxy(real); // 진짜 객체를 프록시로 감쌈
        Client client = new Client(proxy);       // 클라이언트는 프록시를 '진짜인 줄' 주입받음(코드 동일)
        client.run();

        System.out.println("\n=> order/findStock 호출 앞뒤로 begin/end 로그가 붙었다(부가 기능).");
        System.out.println("   LogProxy는 호출을 막지 않고 항상 진짜 객체에 위임한다. Client/Real 코드는 예제1과 동일.");
    }
}
