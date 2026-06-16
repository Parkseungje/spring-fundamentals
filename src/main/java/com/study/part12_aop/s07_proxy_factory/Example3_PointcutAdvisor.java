package com.study.part12_aop.s07_proxy_factory;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * [12.7] 예제3 — 3대 개념: Pointcut("어디에") + Advice("무엇을") = Advisor.
 *
 * 예제1·2의 addAdvice는 '모든 메서드'에 부가 기능을 붙였다. 하지만 실무에선 "특정 메서드에만" 적용하고
 * 싶다(예: order에는 로그, findStock에는 안 함). 그 '어디에'를 담당하는 게 Pointcut이다.
 *  - Pointcut: 어디에 적용할지(메서드 필터). 실무 표준은 AspectJExpressionPointcut(execution(...) 표현식).
 *  - Advice: 어떤 부가 로직인지(LogAdvice).
 *  - Advisor: Pointcut + Advice를 한 쌍으로 묶은 것(= "이 조건의 메서드에 이 로직을 적용").
 *
 * 가설: execution(* order(..)) Pointcut + LogAdvice를 Advisor로 묶어 적용하면, order() 호출엔 로그가
 * 붙고 findStock() 호출엔 안 붙는다(Pointcut이 걸러내므로).
 */
public class Example3_PointcutAdvisor {

    public static void main(String[] args) {
        System.out.println("== [Advisor] Pointcut(어디에) + Advice(무엇을) ==");

        // Pointcut: "이름이 order인 메서드"에만 매칭(AspectJ 표현식).
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* order(..))"); // order(..)에만 적용, findStock은 제외

        // Advisor = Pointcut + Advice
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, new LogAdvice());

        ProxyFactory pf = new ProxyFactory(new RealOrderService());
        pf.addAdvisor(advisor); // addAdvice(전부)가 아니라 addAdvisor(조건부)
        OrderService proxy = (OrderService) pf.getProxy();

        System.out.println("[order() 호출] -> Pointcut 매칭 -> 로그 붙음");
        proxy.order("노트북");

        System.out.println("[findStock() 호출] -> Pointcut 불일치 -> 로그 안 붙음(부가 기능 미적용)");
        proxy.findStock("노트북");

        System.out.println("\n=> 같은 프록시인데 order에만 begin/end가 붙고 findStock엔 안 붙었다. Pointcut이 '어디에'를 걸러낸다.");
        System.out.println("   Pointcut+Advice=Advisor. 이 3대 개념이 Spring AOP의 뼈대다(다음 12.8에서 빈 자동 프록시로 이어짐).");
    }
}
