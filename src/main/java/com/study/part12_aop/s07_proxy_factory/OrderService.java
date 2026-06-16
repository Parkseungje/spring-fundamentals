package com.study.part12_aop.s07_proxy_factory;

/**
 * [12.7] 인터페이스가 있는 서비스 — ProxyFactory가 JDK 동적 프록시를 선택하는 대상.
 *
 * 메서드를 둘 둔 이유: Example3에서 Pointcut("어디에")으로 order에만 부가 기능을 적용하고 findStock에는
 * 적용하지 않는 '필터링'을 보여주기 위함이다.
 */
public interface OrderService {

    String order(String item);

    int findStock(String item);
}
