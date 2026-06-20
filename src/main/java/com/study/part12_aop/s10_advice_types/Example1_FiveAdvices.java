package com.study.part12_aop.s10_advice_types;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.10] 예제1 — 어드바이스 5종과 실행 순서(정상 흐름).
 *
 * 어드바이스 5종:
 *  - @Around: 가장 강력. 타겟 호출 전후 + 예외 + proceed 호출 여부 + 반환/예외 변환까지 제어.
 *  - @Before: 타겟 호출 '전'.
 *  - @AfterReturning: 타겟이 '정상 반환'한 뒤(반환값 접근 가능).
 *  - @AfterThrowing: 타겟이 '예외'를 던졌을 때.
 *  - @After: 정상이든 예외든 '항상'(finally 같은 것).
 *
 * 정상 호출 시 실행 순서(스프링 기준): Around(전) -> Before -> [타겟] -> AfterReturning -> After -> Around(후).
 * (@Around가 proceed 앞뒤로 가장 바깥을 감싸고, 그 안에서 Before/After 계열이 동작.)
 *
 * 가설: order() 한 번 호출에 다섯 어드바이스가 위 순서로 찍힌다(AfterThrowing은 정상이라 안 찍힘).
 */
public class Example1_FiveAdvices {

    @Aspect
    static class AllAdvices {
        @Around("execution(* order(..))")
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            System.out.println("  [Around-전]");
            Object result = pjp.proceed();
            System.out.println("  [Around-후] 반환=" + result);
            return result;
        }

        @Before("execution(* order(..))")
        public void before() {
            System.out.println("  [Before]");
        }

        @AfterReturning(pointcut = "execution(* order(..))", returning = "result")
        public void afterReturning(Object result) {
            System.out.println("  [AfterReturning] 반환=" + result);
        }

        @AfterThrowing(pointcut = "execution(* order(..))", throwing = "ex")
        public void afterThrowing(Exception ex) {
            System.out.println("  [AfterThrowing] 예외=" + ex.getMessage()); // 정상 호출이라 안 찍힘
        }

        @After("execution(* order(..))")
        public void after() {
            System.out.println("  [After] (finally)");
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean AllAdvices allAdvices() { return new AllAdvices(); }
    }

    public static void main(String[] args) {
        System.out.println("== [어드바이스 5종] 정상 흐름 실행 순서 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        OrderService bean = ctx.getBean(OrderService.class);
        bean.order("노트북");
        ctx.close();
        System.out.println("\n=> 순서: Around(전) -> Before -> 타겟 -> AfterReturning -> After -> Around(후).");
        System.out.println("   정상 반환이라 AfterThrowing은 실행되지 않았다.");
    }
}
