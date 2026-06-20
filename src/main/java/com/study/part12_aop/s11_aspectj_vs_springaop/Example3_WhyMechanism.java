package com.study.part12_aop.s11_aspectj_vs_springaop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.11] 예제3 — 왜 내부 호출이 안 되나: '프록시'와 '진짜 객체'는 서로 다른 두 객체다.
 *
 * 핵심 원인: 자동 프록시 생성기는 진짜 객체(target)를 감싼 '별도의 프록시 객체'를 만들어 빈으로 등록한다.
 * 클라이언트가 가진 건 프록시고, 어드바이스는 프록시가 위임할 때 끼어든다. 그런데 진짜 객체 '안'에서
 * 쓰는 this는 어디까지나 '진짜 객체 자신'이라 프록시의 존재를 모른다. 그래서 this.method()는 프록시를
 * 거치지 않는다.
 *
 * 이 예제는 그 사실을 '클래스 정체성'으로 보여준다:
 *  - 빈(클라이언트가 받은 것).getClass() -> 프록시 클래스(...$$SpringCGLIB...)
 *  - 메서드 안의 this.getClass()       -> 진짜 객체 클래스(CallService)
 * 둘이 다른 것 = 프록시와 타겟이 별개 객체임을 증명. 그래서 타겟 내부의 this 호출은 프록시를 못 거친다.
 */
public class Example3_WhyMechanism {

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean CallService callService() { return new CallService(); }
        @Bean LogAspect logAspect() { return new LogAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [원인] 프록시 != 진짜 객체 (정체성 비교) ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        CallService bean = ctx.getBean(CallService.class);

        System.out.println("클라이언트가 받은 빈.getClass() = " + bean.getClass().getSimpleName() + "  <- 프록시");
        System.out.println("external() 호출 -> 메서드 안 this 를 확인:");
        bean.external(); // 메서드 내부에서 this.getClass()를 찍음 -> 진짜 객체(CallService)

        ctx.close();
        System.out.println("\n=> 빈(프록시)과 메서드 안 this(진짜 객체 CallService)의 클래스가 다르다 = 둘은 별개 객체.");
        System.out.println("   그래서 진짜 객체 내부의 this.method() 호출은 프록시를 거치지 않아 어드바이스가 빠진다(예제2).");
        System.out.println("   순수 AspectJ(컴파일/로딩 시 위빙)는 프록시가 아니라 바이트코드에 직접 짜 넣어 이 한계가 없다.");
    }
}
