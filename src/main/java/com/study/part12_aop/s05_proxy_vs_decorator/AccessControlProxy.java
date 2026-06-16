package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 프록시 — '접근 제어': 권한이 있어야만 진짜 객체에 접근시킨다.
 *
 * 데코레이터(UpperCase/Bracket)와 '구조'는 똑같다 — 같은 TextSource 구현 + target 보유 + 호출 가로채기.
 * 그러나 '의도'가 다르다: 결과를 꾸미는 게 아니라, 진짜 객체로의 '접근 여부'를 통제한다.
 *  - 권한 있음 -> 진짜 객체에 위임(결과를 가공하지 않는다).
 *  - 권한 없음 -> 진짜 객체를 '아예 부르지 않고' 예외/차단(접근 제어의 핵심).
 * 캐싱·지연 로딩도 같은 부류다(호출 자체를 제어). 이 의도 차이가 프록시 패턴 vs 데코레이터 패턴의 본질이다.
 */
public class AccessControlProxy implements TextSource {

    private final TextSource target;
    private final boolean authorized; // 접근 권한 여부

    public AccessControlProxy(TextSource target, boolean authorized) {
        this.target = target;
        this.authorized = authorized;
    }

    @Override
    public String read() {
        if (!authorized) {
            // 접근 제어: 권한 없으면 진짜 객체를 부르지 않고 차단한다(데코레이터엔 없는 동작).
            System.out.println("    [프록시:접근제어] 권한 없음 -> 진짜 객체 호출 차단!");
            throw new SecurityException("접근 권한이 없습니다");
        }
        System.out.println("    [프록시:접근제어] 권한 확인됨 -> 위임");
        return target.read(); // 권한 있을 때만 위임(결과는 가공하지 않음)
    }
}
