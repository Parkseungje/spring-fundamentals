package com.study.part12_aop.s10_advice_types;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.10] 예제3 — @Pointcut 분리·재사용(DRY)과 &&/||/! 조합.
 *
 * 예제1·2는 어드바이스마다 "execution(...)"를 직접 적었다. 여기서는 Pointcuts 클래스에 이름 붙인 포인트컷을
 * 정의하고, 어드바이스들이 그 '이름'을 참조해 재사용한다(문자열 중복 제거). 다른 클래스의 포인트컷은
 * 전체 경로(패키지.클래스.메서드())로 참조한다.
 *
 * 이 예제는 두 어드바이스가 같은 포인트컷 이름을 공유하고, 조합 포인트컷(everythingButFail = 전체 && !fail)도
 * 사용해, order에는 적용되고 fail에는 적용되지 않음을 보여준다.
 *
 * 예제1·2와의 차이: 포인트컷 표현식을 인라인으로 반복하지 않고 '이름'으로 분리·조합한다(유지보수성).
 */
public class Example3_PointcutReuse {

    private static final String PC = "com.study.part12_aop.s10_advice_types.Pointcuts.";

    @Aspect
    static class LogAspect {
        // 같은 포인트컷 이름(orders)을 두 어드바이스가 재사용
        @Before(PC + "orders()")
        public void beforeOrders() {
            System.out.println("  [Before] orders() 포인트컷 재사용 #1");
        }

        // 조합 포인트컷: 전체 메서드 && !fail -> order에는 적용, fail에는 미적용
        @Before(PC + "everythingButFail()")
        public void beforeEverythingButFail() {
            System.out.println("  [Before] everythingButFail() (전체 && !fail)");
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean OrderService orderService() { return new RealOrderService(); }
        @Bean LogAspect logAspect() { return new LogAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [@Pointcut 분리·재사용 + &&/||/! 조합] ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        OrderService bean = ctx.getBean(OrderService.class);

        System.out.println("[order()] -> orders() 와 everythingButFail() 둘 다 매칭");
        bean.order("노트북");

        System.out.println("[fail()] -> orders() 불일치, everythingButFail()도 !fail 이라 미적용");
        try {
            bean.fail("노트북");
        } catch (RuntimeException ignored) {
        }

        ctx.close();
        System.out.println("\n=> 포인트컷을 이름으로 분리해 여러 어드바이스가 재사용(DRY). &&/||/! 로 조합해 fail만 제외했다.");
    }
}
