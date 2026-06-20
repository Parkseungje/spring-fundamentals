package com.study.part12_aop.s07_proxy_factory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * [12.7] 예제4 — '프록시 1개 + 어드바이저 N개'가 등록 순서대로 줄지어 도는 것(체인) + NameMatchMethodPointcut.
 *
 * 1-4에서 "target 1개당 프록시 1개, 그 안에 어드바이저 여러 개를 체인으로 둔다"고 했다. 이를 코드로 확인한다.
 * 어드바이저를 2개(보안 -> 로그) 등록하면, 호출은 등록 순서대로 '바깥 -> 안쪽'으로 진입하고, 진짜 메서드를
 * 부른 뒤 '안쪽 -> 바깥'으로 복귀한다(양파 껍질처럼). 프록시는 '하나'다(보안용/로그용 프록시 2개가 아님).
 *
 * 또한 Pointcut을 AspectJ 표현식이 아니라 NameMatchMethodPointcut(메서드 이름 매칭, 가장 단순)으로 만들어,
 * "Pointcut = AspectJ"가 아니라 여러 구현 중 하나임을 보여준다. (Pointcut은 내부적으로 ClassFilter +
 * MethodMatcher 2단 필터 — 여기선 클래스는 전부 허용, 메서드 이름만 매칭.)
 *
 * 가설: order() 호출 시 [보안 진입] -> [로그 시작] -> 비즈니스 -> [로그 종료] -> [보안 복귀] 순으로 찍힌다.
 * (findStock은 Pointcut에 없어 어드바이스가 안 붙는다.)
 */
public class Example4_AdvisorChainOrder {

    public static void main(String[] args) {
        System.out.println("== [어드바이저 체인] 프록시 1개에 어드바이저 2개(보안 -> 로그) ==");

        ProxyFactory pf = new ProxyFactory(new RealOrderService());

        // Pointcut: 메서드 이름이 'order'인 것만(AspectJ 없이 이름 매칭). 클래스는 기본적으로 전부 허용.
        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.addMethodName("order");

        // 어드바이저를 '등록한 순서대로' 체인이 된다: 먼저 등록한 보안이 바깥, 그다음 로그가 안쪽.
        pf.addAdvisor(new DefaultPointcutAdvisor(pointcut, new SecurityAdvice())); // 1순위(바깥)
        pf.addAdvisor(new DefaultPointcutAdvisor(pointcut, new LogAdvice()));      // 2순위(안쪽)

        OrderService proxy = (OrderService) pf.getProxy();
        System.out.println("프록시 실제 클래스 = " + proxy.getClass().getName() + " (프록시는 '하나')");

        System.out.println("\n[order() 호출] — 어드바이저 2개가 줄지어 돈다:");
        proxy.order("노트북");

        System.out.println("\n[findStock() 호출] — Pointcut(order)에 없어 어드바이스 미적용:");
        proxy.findStock("노트북");

        System.out.println("\n=> 보안(바깥) -> 로그(안쪽) -> 비즈니스 -> 로그 -> 보안 순(양파 껍질). 프록시는 1개, 어드바이저만 N개.");
        System.out.println("   순서는 등록 순서(또는 @Order)로 정해진다. Pointcut은 NameMatch로도 만들 수 있다(AspectJ 전용 아님).");
    }
}
