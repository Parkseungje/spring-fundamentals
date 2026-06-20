package com.study.part12_aop.s08_bean_postprocessor;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * [12.8] 예제3 — 컴포넌트 스캔된 빈(@Service)도 자동 프록시 생성기가 프록시로 교체한다.
 *
 * 12.7의 ProxyFactory로는 @Service로 스캔되어 컨테이너가 직접 만든 빈을 바꿔치기하기 어려웠다(끼어들 틈이
 * 없음). 자동 프록시 생성기는 '빈 후처리' 시점에 끼어들므로, 스캔된 빈이든 @Bean이든 가리지 않고 Pointcut에
 * 매칭되면 프록시로 교체한다.
 *
 * 가설: @Service ScannedOrderService를 컴포넌트 스캔으로 등록하고 Advisor만 추가하면, getBean 시 '스캔된
 * 빈'이 프록시로 바뀌어 있고 order에 로그가 붙는다.
 *
 * 예제2와의 차이: 대상 빈을 @Bean으로 '직접 등록'하지 않고 @Service '컴포넌트 스캔'으로 등록한다 -- 즉
 * ProxyFactory가 손대기 어려웠던 케이스를 자동 프록시 생성기가 해결함을 보여준다.
 */
public class Example3_ComponentScannedBean {

    @Configuration
    @EnableAspectJAutoProxy
    // 이 패키지를 스캔하되, @Configuration(예제1/2의 내부 설정)은 제외해 빈 충돌을 막는다.
    @ComponentScan(
            basePackageClasses = Example3_ComponentScannedBean.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class))
    static class Config {
        @Bean
        Advisor logAdvisor() {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* order(..))");
            return new DefaultPointcutAdvisor(pointcut, new LogAdvice());
        }
    }

    public static void main(String[] args) {
        System.out.println("== [자동 프록시 생성기] 컴포넌트 스캔된 @Service도 프록시로 교체 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

        // JDK 동적 프록시는 인터페이스 타입($ProxyN)이므로 구체 클래스(ScannedOrderService)가 아니라
        // 인터페이스(OrderService)로 꺼낸다. (프록시가 ScannedOrderService를 '상속'한 게 아니라 OrderService를 '구현')
        OrderService bean = ctx.getBean(OrderService.class);
        System.out.println("스캔된 빈의 실제 클래스 = " + bean.getClass().getName()); // 프록시

        System.out.println("[order()]");
        bean.order("노트북");

        ctx.close();
        System.out.println("\n=> @Service로 스캔된 빈인데도 자동 프록시 생성기가 프록시로 교체했다(ProxyFactory가 못하던 것).");
        System.out.println("   이것이 @Transactional·@Cacheable 등이 우리 서비스 빈에 '저절로' 적용되는 메커니즘이다.");
    }
}
