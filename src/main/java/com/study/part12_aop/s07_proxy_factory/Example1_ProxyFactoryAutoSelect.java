package com.study.part12_aop.s07_proxy_factory;

import org.springframework.aop.framework.ProxyFactory;

/**
 * [12.7] 예제1 — ProxyFactory가 JDK/CGLIB를 '자동 선택'한다(+ 강제 옵션).
 *
 * 12.6에서 우리는 "인터페이스면 JDK, 구체 클래스면 CGLIB"를 직접 갈라 코딩해야 했다. ProxyFactory는
 * target만 넘기면 알아서 고른다:
 *  - target에 인터페이스가 있으면 -> JDK 동적 프록시($ProxyN)
 *  - 인터페이스가 없으면 -> CGLIB(클래스 상속, ...$$SpringCGLIB...)
 *  - setProxyTargetClass(true)면 -> 인터페이스가 있어도 강제로 CGLIB
 * Spring Boot는 기본적으로 CGLIB를 쓴다(일관성). 이 예제는 세 경우의 '프록시 실제 클래스'로 선택 결과를 확인한다.
 */
public class Example1_ProxyFactoryAutoSelect {

    public static void main(String[] args) {
        System.out.println("== [ProxyFactory] target에 따라 JDK/CGLIB 자동 선택 ==");

        // (1) 인터페이스 있는 target -> JDK 동적 프록시
        ProxyFactory pf1 = new ProxyFactory(new RealOrderService());
        pf1.addAdvice(new LogAdvice());
        OrderService jdkProxy = (OrderService) pf1.getProxy();
        jdkProxy.order("노트북");
        System.out.println("  (1) 인터페이스 target -> " + jdkProxy.getClass().getName());

        // (2) 인터페이스 없는 구체 클래스 target -> CGLIB
        ProxyFactory pf2 = new ProxyFactory(new ConcreteService());
        pf2.addAdvice(new LogAdvice());
        ConcreteService cglibProxy = (ConcreteService) pf2.getProxy();
        cglibProxy.run();
        System.out.println("  (2) 구체 클래스 target -> " + cglibProxy.getClass().getName());

        // (3) 인터페이스 있어도 proxyTargetClass=true -> 강제 CGLIB (Spring Boot 기본 동작과 동일)
        ProxyFactory pf3 = new ProxyFactory(new RealOrderService());
        pf3.setProxyTargetClass(true); // 강제 CGLIB
        pf3.addAdvice(new LogAdvice());
        OrderService forcedCglib = (OrderService) pf3.getProxy();
        forcedCglib.order("마우스");
        System.out.println("  (3) 인터페이스+proxyTargetClass=true -> " + forcedCglib.getClass().getName());

        System.out.println("\n=> (1)은 $ProxyN(JDK), (2)·(3)은 SpringCGLIB. target과 옵션만으로 자동/강제 선택된다.");
        System.out.println("   12.6처럼 JDK/CGLIB를 직접 갈라 쓸 필요가 없다(ProxyFactory가 통합).");
    }
}
