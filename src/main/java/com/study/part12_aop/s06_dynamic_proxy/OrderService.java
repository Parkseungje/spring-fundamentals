package com.study.part12_aop.s06_dynamic_proxy;

/**
 * [12.6] 인터페이스가 있는 서비스 1 — JDK 동적 프록시 대상.
 *
 * JDK 동적 프록시는 '인터페이스'를 기반으로 런타임에 프록시를 만든다. 그래서 인터페이스가 필요하다.
 * MemberService와 함께, "핸들러 하나로 서로 다른 인터페이스의 프록시를 모두 만들 수 있다"를 보여주는 데 쓴다.
 */
public interface OrderService {

    String order(String item);
}
