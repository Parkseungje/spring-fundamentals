package com.study.part12_aop.s06_dynamic_proxy;

/**
 * [12.6] 인터페이스가 있는 서비스 2 — JDK 동적 프록시 대상.
 *
 * OrderService와 '다른' 인터페이스다. Example2에서 '같은 InvocationHandler 클래스 하나'로 이 둘의 프록시를
 * 모두 만들 수 있음을 보여, "구현체가 100개여도 핸들러 1개"라는 JDK 동적 프록시의 이점을 드러낸다.
 */
public interface MemberService {

    void join(String name);
}
