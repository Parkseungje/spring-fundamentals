package com.study.part12_aop.s07_proxy_factory;

import org.springframework.aop.framework.ProxyFactory;

/**
 * [12.7] 예제2 — Advice 하나로 JDK·CGLIB 양쪽을 동일하게 처리(핸들러 API 통합).
 *
 * 12.6의 불편: 같은 '로그 추가'를 하려 해도 JDK는 InvocationHandler, CGLIB는 MethodInterceptor로 코드를
 * 따로 써야 했다. ProxyFactory는 이를 'Advice'(org.aopalliance.intercept.MethodInterceptor)로 통합한다.
 *
 * 이 예제의 가설: '같은 LogAdvice 인스턴스'를 인터페이스 target(=JDK로 감싸짐)과 구체 클래스 target
 * (=CGLIB로 감싸짐)에 똑같이 addAdvice 해도, 둘 다 동일하게 로그가 붙는다. 즉 개발자는 프록시 생성
 * 방식(JDK/CGLIB)을 신경 쓰지 않고 Advice 하나만 작성하면 된다.
 *
 * 예제1과의 차이: 예제1은 '어떤 프록시가 선택되나'(클래스 이름)에 초점, 예제2는 '같은 Advice가 양쪽에서
 * 그대로 동작하나'(통합)에 초점.
 */
public class Example2_AdviceUnified {

    public static void main(String[] args) {
        System.out.println("== [Advice 통합] 같은 LogAdvice를 JDK·CGLIB 양쪽에 그대로 적용 ==");

        LogAdvice sharedAdvice = new LogAdvice(); // Advice는 하나만 만든다

        // JDK 쪽(인터페이스 target)
        ProxyFactory pfJdk = new ProxyFactory(new RealOrderService());
        pfJdk.addAdvice(sharedAdvice);
        OrderService jdk = (OrderService) pfJdk.getProxy();
        System.out.println("[JDK 프록시] " + jdk.getClass().getSimpleName());
        jdk.order("노트북");

        // CGLIB 쪽(구체 클래스 target) — '동일한' sharedAdvice 재사용
        ProxyFactory pfCglib = new ProxyFactory(new ConcreteService());
        pfCglib.addAdvice(sharedAdvice);
        ConcreteService cglib = (ConcreteService) pfCglib.getProxy();
        System.out.println("[CGLIB 프록시] " + cglib.getClass().getSimpleName());
        cglib.run();

        System.out.println("\n=> 프록시 생성 방식이 JDK/CGLIB로 다른데도, '같은 Advice 하나'가 양쪽에서 동일하게 로그를 붙였다.");
        System.out.println("   InvocationHandler vs MethodInterceptor를 갈라 쓰던 12.6의 불편이 사라졌다(통합).");
    }
}
