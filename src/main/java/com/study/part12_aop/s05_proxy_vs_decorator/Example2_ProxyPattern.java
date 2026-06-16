package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 예제2 — 프록시 패턴: '접근 제어'. 권한에 따라 진짜 호출을 통과/차단.
 *
 * 가설: AccessControlProxy로 감싸면 권한이 있을 때만 진짜 객체에 위임하고, 없으면 진짜 객체를 부르지
 * 않고 차단한다(예외). 결과를 가공하지는 않는다 -- 데코레이터와 다른 '의도'(접근 제어).
 *
 * 예제1과의 대비: 코드 구조는 데코레이터와 똑같다(같은 인터페이스 + target 위임). 그러나 동작의 의도가
 * "결과 꾸미기"가 아니라 "접근 허용 여부 결정"이다. 같은 모양, 다른 의도 -- 이것이 12.5의 핵심.
 */
public class Example2_ProxyPattern {

    public static void main(String[] args) {
        System.out.println("== [프록시] 접근 제어: 권한 있음 vs 없음 ==");

        TextSource plain = new PlainTextSource("secret-data");

        System.out.println("[권한 있음]");
        TextSource allowed = new AccessControlProxy(plain, true);
        System.out.println("  결과 = " + allowed.read());

        System.out.println("[권한 없음]");
        TextSource denied = new AccessControlProxy(plain, false);
        try {
            denied.read();
        } catch (SecurityException e) {
            System.out.println("  차단됨 -> " + e.getMessage() + " (진짜 객체는 호출조차 안 됨)");
        }

        System.out.println("\n=> 권한 있으면 위임(결과 그대로), 없으면 진짜 호출을 막고 차단. 결과를 '꾸미지' 않는다.");
        System.out.println("   데코레이터(예제1)와 코드 모양은 같지만 의도가 '접근 제어'다(캐싱·지연 로딩도 같은 부류).");
    }
}
