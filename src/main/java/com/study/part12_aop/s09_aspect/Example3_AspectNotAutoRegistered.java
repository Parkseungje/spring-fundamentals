package com.study.part12_aop.s09_aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.9] 예제3 — ★ 함정: @Aspect를 붙여도 '빈 등록'을 안 하면 적용되지 않는다.
 *
 * @Aspect는 "이 클래스가 애스펙트다"라는 표시일 뿐, '자동 빈 등록'은 아니다. 스프링 빈으로 등록돼야
 * (@Bean/@Component/@Import) 자동 프록시 생성기가 이를 Advisor로 변환해 적용한다. 초보자가 자주 빠지는
 * 함정 — @Aspect만 붙이고 빈 등록을 잊으면 "AOP가 왜 안 먹지?" 하게 된다.
 *
 * 이 예제는 그 함정을 '재현'하고(미등록 -> 로그 안 붙음), 바로 '해결'한다(등록 -> 로그 붙음).
 * Config_Broken: @Aspect 클래스를 @Bean으로 등록하지 않음 -> 부가 기능 미적용.
 * Config_Fixed:  @Aspect 클래스를 @Bean으로 등록 -> 부가 기능 적용.
 */
public class Example3_AspectNotAutoRegistered {

    @Aspect
    static class LogAspect {
        @Around("execution(* order(..))")
        public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
            System.out.println("--> [@Aspect] 시작");
            Object result = joinPoint.proceed();
            System.out.println("<-- [@Aspect] 종료");
            return result;
        }
    }

    // 함정: @Aspect 클래스를 빈으로 등록하지 않음 (orderService만 등록)
    @Configuration
    @EnableAspectJAutoProxy
    static class Config_Broken {
        @Bean OrderService orderService() { return new RealOrderService(); }
        // LogAspect 빈 등록 없음! -> AOP 미적용
    }

    // 해결: @Aspect 클래스도 빈으로 등록
    @Configuration
    @EnableAspectJAutoProxy
    static class Config_Fixed {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean LogAspect logAspect() { return new LogAspect(); } // 이 한 줄이 핵심
    }

    public static void main(String[] args) {
        System.out.println("== [함정] @Aspect 미등록 -> AOP 미적용 ==");
        AnnotationConfigApplicationContext broken = new AnnotationConfigApplicationContext(Config_Broken.class);
        OrderService b = broken.getBean(OrderService.class);
        System.out.println("실제 클래스 = " + b.getClass().getSimpleName() + " (프록시 아님)");
        b.order("노트북"); // 로그 안 붙음
        broken.close();

        System.out.println("\n== [해결] @Aspect를 @Bean으로 등록 ==");
        AnnotationConfigApplicationContext fixed = new AnnotationConfigApplicationContext(Config_Fixed.class);
        OrderService f = fixed.getBean(OrderService.class);
        System.out.println("실제 클래스 = " + f.getClass().getSimpleName() + " (프록시)");
        f.order("노트북"); // 로그 붙음
        fixed.close();

        System.out.println("\n=> @Aspect만으론 부족하고 '빈 등록'이 있어야 적용된다. (실무에선 보통 @Component를 함께 붙임)");
    }
}
