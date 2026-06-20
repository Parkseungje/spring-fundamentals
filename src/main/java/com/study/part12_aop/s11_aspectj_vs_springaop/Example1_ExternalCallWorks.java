package com.study.part12_aop.s11_aspectj_vs_springaop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.11] 예제1 — 외부 호출은 프록시를 거치므로 어드바이스가 정상 적용된다(기준선).
 *
 * 클라이언트가 빈(프록시)의 external()을 직접 부르면 프록시 -> 어드바이스 -> 진짜 객체 순으로 흘러
 * [AOP] 로그가 붙는다. 예제2(내부 호출)와 대조하기 위한 '정상 동작' 기준이다.
 */
public class Example1_ExternalCallWorks {

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean CallService callService() { return new CallService(); }
        @Bean LogAspect logAspect() { return new LogAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [외부 호출] 프록시 경유 -> 어드바이스 적용 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        CallService bean = ctx.getBean(CallService.class);
        System.out.println("빈 실제 클래스 = " + bean.getClass().getName() + " (프록시)");
        System.out.println("[bean.external() 호출]");
        bean.external(); // 프록시를 통한 외부 호출 -> 어드바이스 붙음
        ctx.close();
        System.out.println("\n=> 외부에서 프록시로 호출하니 [AOP] 로그가 붙었다(정상). 예제2의 내부 호출과 비교하라.");
    }
}
