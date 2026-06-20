package com.study.part12_aop.s09_aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Arrays;

/**
 * [12.9] 예제2 — ProceedingJoinPoint로 '조인 포인트' 정보 읽기 + AOP 용어 7가지 매핑.
 *
 * @Around의 인자 ProceedingJoinPoint는 '지금 가로챈 그 호출 지점(조인 포인트)'의 정보를 담는다.
 * 메서드 이름·인자·타겟 등을 읽어 로그/측정에 활용한다. 그리고 proceed()로 타겟을 호출한다.
 *
 * AOP 7대 용어를 이 예제에 대응시키면:
 *  - 조인 포인트(Join point): 부가 기능을 적용할 수 있는 모든 지점(메서드 호출 등). 여기 joinPoint가 그 한 지점.
 *  - 포인트컷(Pointcut): 조인 포인트 중 실제 적용할 곳 선택 -> @Around("execution(* order(..))").
 *  - 어드바이스(Advice): 적용할 부가 기능 -> log 메서드 본문(시간 측정).
 *  - 애스펙트(Aspect): 포인트컷+어드바이스 모듈 -> @Aspect TimingAspect.
 *  - 타겟(Target): 부가 기능이 적용되는 실제 객체 -> RealOrderService.
 *  - 위빙(Weaving): 포인트컷으로 어드바이스를 결합 -> 스프링은 빈 후처리 시점에 프록시로 위빙.
 *  - AOP 프록시: JDK 동적 프록시 또는 CGLIB -> bean.getClass()로 확인.
 *
 * 예제1과의 차이: 예제1은 "@Aspect가 적용된다"가 초점, 예제2는 "조인 포인트 정보 활용 + 용어 매핑"이 초점.
 */
public class Example2_JoinPointInfo {

    @Aspect
    static class TimingAspect {
        @Around("execution(* order(..))")
        public Object timing(ProceedingJoinPoint joinPoint) throws Throwable {
            // 조인 포인트 정보 읽기
            String method = joinPoint.getSignature().toShortString(); // 메서드 시그니처
            Object[] args = joinPoint.getArgs();                       // 인자들
            System.out.println("--> 조인포인트: " + method + " 인자=" + Arrays.toString(args));

            long start = System.nanoTime();
            Object result = joinPoint.proceed();                       // 타겟 호출
            long tookMicros = (System.nanoTime() - start) / 1000;
            System.out.println("<-- 반환=" + result + " (" + tookMicros + "us)");
            return result;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean TimingAspect timingAspect() { return new TimingAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [조인 포인트 정보 + 7대 용어] ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

        OrderService bean = ctx.getBean(OrderService.class);
        System.out.println("AOP 프록시 = " + bean.getClass().getName()); // 7대 용어의 'AOP 프록시'
        bean.order("노트북");

        ctx.close();
        System.out.println("\n=> ProceedingJoinPoint에서 메서드명·인자·반환값을 읽어 부가 기능에 활용했다.");
        System.out.println("   포인트컷/어드바이스/애스펙트/타겟/위빙/AOP프록시/조인포인트 7대 용어가 이 흐름에 다 대응된다.");
    }
}
