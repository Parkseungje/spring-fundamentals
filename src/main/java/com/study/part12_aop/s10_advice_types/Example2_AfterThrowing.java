package com.study.part12_aop.s10_advice_types;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.10] 예제2 — 예외 흐름: @AfterThrowing vs @AfterReturning, 그리고 @After(finally).
 *
 * 예제1은 정상 흐름이라 AfterThrowing이 안 찍혔다. 여기서는 예외를 던지는 fail()을 호출해 차이를 본다.
 *  - 타겟이 예외를 던지면: Before -> [타겟 예외] -> AfterThrowing -> After. AfterReturning은 '안' 찍힌다.
 *  - @After는 정상/예외 무관하게 항상 실행(finally)이라 예외 때도 찍힌다.
 *
 * 예제1과의 차이: 정상(order) vs 예외(fail). 둘을 비교하면 "정상이면 AfterReturning, 예외면 AfterThrowing,
 * 둘 다 After"라는 분기가 또렷해진다. 이 정상/예외 분기가 PART 11.5 @Transactional의 커밋/롤백 결정과 같은 자리다.
 */
public class Example2_AfterThrowing {

    @Aspect
    static class AfterAdvices {
        @Before("execution(* fail(..))")
        public void before() { System.out.println("  [Before]"); }

        @AfterReturning("execution(* fail(..))")
        public void afterReturning() { System.out.println("  [AfterReturning] (정상 반환 시만 — 안 찍혀야 정상)"); }

        @AfterThrowing(pointcut = "execution(* fail(..))", throwing = "ex")
        public void afterThrowing(Exception ex) { System.out.println("  [AfterThrowing] 예외=" + ex.getMessage()); }

        @After("execution(* fail(..))")
        public void after() { System.out.println("  [After] (finally — 예외여도 실행)"); }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean AfterAdvices afterAdvices() { return new AfterAdvices(); }
    }

    public static void main(String[] args) {
        System.out.println("== [예외 흐름] AfterThrowing/AfterReturning/After 구분 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        OrderService bean = ctx.getBean(OrderService.class);
        try {
            bean.fail("노트북");
        } catch (RuntimeException e) {
            System.out.println("  (호출부) 예외 받음: " + e.getMessage());
        }
        ctx.close();
        System.out.println("\n=> 예외 시: Before -> 타겟 예외 -> AfterThrowing -> After. AfterReturning은 실행 안 됨.");
        System.out.println("   @After는 정상/예외 무관 항상 실행(finally). 이 분기가 @Transactional 커밋/롤백과 같은 자리.");
    }
}
