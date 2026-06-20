package com.study.part12_aop.s09_aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.9] 예제1 — @Aspect: Pointcut+Advice를 한 클래스에 선언하면 스프링이 Advisor로 자동 변환한다.
 *
 * 12.7~12.8에서는 Pointcut 객체와 Advice 객체를 직접 만들어 DefaultPointcutAdvisor로 조립했다. @Aspect는
 * 그걸 더 선언적으로 쓰게 해준다(AspectJ의 어노테이션을 스프링이 차용).
 *  - @Aspect: 이 클래스가 애스펙트(Pointcut + Advice 모듈)임을 표시.
 *  - @Around("execution(...)"): 포인트컷(어디에) + 어드바이스(무엇을)를 한 메서드에 결합.
 *  - ProceedingJoinPoint.proceed(): 타겟의 실제 메서드를 호출(이 앞뒤를 감싸 부가 기능).
 *
 * ★ 함정(중요): @Aspect를 붙여도 '자동 빈 등록은 아니다'. 스프링 빈으로 등록돼야(@Bean/@Component/@Import)
 * 자동 프록시 생성기가 이를 Advisor로 변환해 적용한다. 여기선 @Bean으로 등록한다(예제3에서 미등록 시 함정 확인).
 *
 * 가설: @Aspect 빈을 등록하면 order에 @Around 로그가 붙는다(자동 프록시 생성기가 Advisor로 변환·적용).
 */
public class Example1_AtAspect {

    @Aspect
    static class LogAspect {
        // 포인트컷(execution(* order(..))) + 어드바이스(아래 본문)를 한 메서드에 결합한 게 @Around.
        @Around("execution(* order(..))")
        public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
            String name = joinPoint.getSignature().getName();
            System.out.println("--> [@Aspect] " + name + " 시작");
            Object result = joinPoint.proceed(); // 타겟 실제 메서드 호출
            System.out.println("<-- [@Aspect] " + name + " 종료");
            return result;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean
        OrderService orderService() {
            return new RealOrderService();
        }

        @Bean // ★ @Aspect도 '빈 등록'을 해줘야 적용된다(자동 등록 아님)
        LogAspect logAspect() {
            return new LogAspect();
        }
    }

    public static void main(String[] args) {
        System.out.println("== [@Aspect] @Around로 Pointcut+Advice를 선언 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

        OrderService bean = ctx.getBean(OrderService.class);
        System.out.println("실제 클래스 = " + bean.getClass().getName());
        System.out.println("[order()]");
        bean.order("노트북");
        System.out.println("[findStock()] -> 포인트컷 불일치");
        bean.findStock("노트북");

        ctx.close();
        System.out.println("\n=> @Aspect 클래스가 Advisor로 자동 변환되어 order에 로그가 붙었다. Pointcut+Advice를 한 클래스로 선언.");
    }
}
