package com.study.part12_aop.s10_advice_types;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.10] 예제4 — 포인트컷 지정자 '@annotation': 커스텀 어노테이션이 붙은 메서드에만 적용.
 *
 * execution(메서드 패턴) 대신 @annotation(LogExecutionTime)을 쓰면, '메서드 이름'이 아니라 '@LogExecutionTime이
 * 붙었는지'로 대상을 고른다. 그래서 개발자가 측정하고 싶은 메서드에만 콕 집어 어노테이션을 달면 된다
 * (실무에서 매우 흔한 패턴 — @LogExecutionTime, @RateLimit, @Audit 등 커스텀 어노테이션 기반 AOP).
 *
 * 가설: @LogExecutionTime이 붙은 slowTask에는 시간 측정 로그가 붙고, 안 붙은 fastTask에는 안 붙는다.
 */
public class Example4_CustomAnnotationPointcut {

    static class TaskService {
        @LogExecutionTime                          // 이 메서드만 AOP 대상(어노테이션으로 표시)
        public void slowTask() {
            sleep(50);
            System.out.println("    [비즈니스] slowTask 완료");
        }

        public void fastTask() {                   // 어노테이션 없음 -> AOP 미적용
            System.out.println("    [비즈니스] fastTask 완료");
        }

        static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
    }

    @Aspect
    static class TimerAspect {
        // execution(메서드 패턴)이 아니라 @annotation(어노테이션 타입)으로 포인트컷을 잡는다.
        @Around("@annotation(com.study.part12_aop.s10_advice_types.LogExecutionTime)")
        public Object measure(ProceedingJoinPoint pjp) throws Throwable {
            long start = System.currentTimeMillis();
            Object result = pjp.proceed();
            System.out.println("  [측정] " + pjp.getSignature().getName()
                    + " 실행시간 = " + (System.currentTimeMillis() - start) + "ms");
            return result;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean TaskService taskService() { return new TaskService(); }
        @Bean TimerAspect timerAspect() { return new TimerAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [@annotation 포인트컷] @LogExecutionTime 붙은 메서드에만 시간 측정 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        TaskService service = ctx.getBean(TaskService.class);

        System.out.println("[slowTask() — @LogExecutionTime 있음]");
        service.slowTask();
        System.out.println("[fastTask() — 어노테이션 없음]");
        service.fastTask();

        ctx.close();
        System.out.println("\n=> @LogExecutionTime이 붙은 slowTask에만 [측정] 로그가 붙고, fastTask엔 안 붙었다.");
        System.out.println("   execution(이름 패턴) 대신 @annotation(어노테이션)으로 '표시한 메서드만' 고르는 커스텀 어노테이션 AOP.");
        System.out.println("   (스프링의 @Transactional/@Cacheable도 결국 이런 '어노테이션 기반' 포인트컷으로 동작한다.)");
    }
}
