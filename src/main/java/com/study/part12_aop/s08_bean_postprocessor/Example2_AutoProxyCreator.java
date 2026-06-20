package com.study.part12_aop.s08_bean_postprocessor;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.8] 예제2 — 자동 프록시 생성기: Advisor만 빈으로 등록하면 매칭되는 빈이 '자동으로' 프록시가 된다.
 *
 * 예제1의 커스텀 후처리기는 "어떤 타입을 어떻게 감쌀지"를 직접 if로 짜야 했다. 스프링은 이 일을 하는
 * 표준 빈 후처리기 'AnnotationAwareAspectJAutoProxyCreator'를 제공한다(spring-boot-starter-aop가 자동 등록,
 * 여기선 @EnableAspectJAutoProxy로 활성화). 이 후처리기는 컨테이너의 '모든 Advisor를 찾아', 각 빈의 메서드가
 * Advisor의 Pointcut에 매칭되면 그 빈을 프록시로 교체한다.
 *
 * 즉 개발자는 'Advisor(Pointcut+Advice)' 하나만 빈으로 등록하면 된다. 대상 빈마다 프록시 설정을 손수 할
 * 필요가 없다(설정 지옥 해소). 가설: order()엔 로그가 붙고 findStock()엔 안 붙는다(Pointcut 매칭 차이).
 *
 * 예제1과의 차이: 후처리기를 '직접' 만들지 않고, 표준 자동 프록시 생성기에 'Advisor'만 맡긴다.
 */
public class Example2_AutoProxyCreator {

    @Configuration
    @EnableAspectJAutoProxy // AnnotationAwareAspectJAutoProxyCreator(빈 후처리기) 활성화
    static class Config {
        @Bean
        OrderService orderService() {
            return new RealOrderService();
        }

        // Advisor 하나만 등록 -> 자동 프록시 생성기가 알아서 매칭 빈을 프록시로 교체
        @Bean
        Advisor logAdvisor() {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* order(..))"); // order에만
            return new DefaultPointcutAdvisor(pointcut, new LogAdvice());
        }
    }

    public static void main(String[] args) {
        System.out.println("== [자동 프록시 생성기] Advisor만 등록하면 매칭 빈이 자동 프록시 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

        OrderService bean = ctx.getBean(OrderService.class);
        System.out.println("실제 클래스 = " + bean.getClass().getName()); // 프록시

        System.out.println("[order()] -> Pointcut 매칭 -> 로그 붙음");
        bean.order("노트북");
        System.out.println("[findStock()] -> Pointcut 불일치 -> 로그 없음");
        bean.findStock("노트북");

        ctx.close();
        System.out.println("\n=> 빈마다 프록시 설정을 안 하고 'Advisor 하나'만 등록했는데 자동 프록시 생성기가 교체했다.");
        System.out.println("   order만 로그가 붙어 Pointcut이 적용됨도 확인. 설정 지옥이 사라졌다.");
    }
}
