package com.study.part12_aop.s09_aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.9] 예제4 — ★ @Around 함정: proceed()를 깜빡하면 타겟 메서드가 아예 실행되지 않는다.
 *
 * @Around는 '직접' 타겟을 호출해야 한다(ProceedingJoinPoint.proceed()). @Before/@After와 달리 타겟 호출
 * 책임이 개발자에게 있다. 그래서 proceed()를 빠뜨리면 비즈니스 로직이 통째로 실행되지 않고, 반환값도 null이 된다.
 *
 * 이 예제는 같은 order()에 대해 두 애스펙트를 비교한다:
 *   - BadAspect : proceed()를 부르지 않음 -> 타겟 order() 미실행 -> "[비즈니스]" 출력 없음 + 반환 null
 *   - GoodAspect: proceed()를 부름        -> 타겟 정상 실행 -> 비즈니스 출력 + 정상 반환
 * (한 번에 한 애스펙트만 등록해 차이를 또렷이 본다.)
 */
public class Example4_ProceedOmittedTrap {

    @Aspect
    static class BadAspect {
        @Around("execution(* order(..))")
        public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
            System.out.println("--> [Around] 시작 (그런데 proceed()를 호출하지 않는다!)");
            // proceed() 누락 -> 타겟 order()가 호출되지 않는다 -> 비즈니스 로직 실행 안 됨
            System.out.println("<-- [Around] 종료 (타겟을 안 불렀다)");
            return null;   // 타겟을 안 불렀으니 돌려줄 게 없다 -> 호출자는 null을 받는다
        }
    }

    @Aspect
    static class GoodAspect {
        @Around("execution(* order(..))")
        public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
            System.out.println("--> [Around] 시작");
            Object result = joinPoint.proceed();   // 타겟 호출(필수!)
            System.out.println("<-- [Around] 종료");
            return result;
        }
    }

    @Configuration @EnableAspectJAutoProxy
    static class BadConfig {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean BadAspect badAspect() { return new BadAspect(); }
    }

    @Configuration @EnableAspectJAutoProxy
    static class GoodConfig {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean GoodAspect goodAspect() { return new GoodAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [함정] proceed() 누락 -> 타겟 미실행 ==");
        AnnotationConfigApplicationContext bad = new AnnotationConfigApplicationContext(BadConfig.class);
        OrderService b = bad.getBean(OrderService.class);
        String r1 = b.order("노트북");
        System.out.println("반환값 = " + r1 + "   <- null! (비즈니스 '[비즈니스] 주문 처리'가 안 찍힘 = 타겟 미실행)");
        bad.close();

        System.out.println("\n== [정상] proceed() 호출 ==");
        AnnotationConfigApplicationContext good = new AnnotationConfigApplicationContext(GoodConfig.class);
        OrderService g = good.getBean(OrderService.class);
        String r2 = g.order("노트북");
        System.out.println("반환값 = " + r2 + "   <- 정상(타겟 실행됨)");
        good.close();

        System.out.println("\n=> @Around는 proceed()로 '직접' 타겟을 불러야 한다. 빠뜨리면 비즈니스가 통째로 실행 안 되고 null 반환.");
        System.out.println("   (@Before/@After는 타겟 호출을 스프링이 하므로 이 함정이 없다 — 그래서 단순 케이스엔 @Before/@After가 안전.)");
    }
}
