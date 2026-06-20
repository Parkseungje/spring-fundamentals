package com.study.part12_aop.s11_aspectj_vs_springaop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [12.11] 예제2 — ★ 내부 호출(this.external())은 프록시를 우회해 어드바이스가 적용되지 않는다.
 *
 * internalCaller()를 외부에서(프록시로) 호출하면, internalCaller 자체는 프록시를 거치지만(어드바이스 대상은
 * external뿐이라 internalCaller엔 원래 로그 없음), 그 안에서 부르는 this.external()은 '진짜 객체'에서 바로
 * 호출된다 -> 프록시를 안 거치므로 external의 [AOP] 로그가 '안' 붙는다.
 *
 * 예제1과의 차이: external을 '외부에서 직접'(예제1) vs 'internalCaller 내부에서'(예제2) 호출. 같은 external인데
 * 호출 경로에 따라 어드바이스 적용 여부가 갈린다. 이것이 Spring AOP(런타임 프록시)의 대표 한계다.
 *
 * 이 문제의 해결책(자기 자신 주입, AopContext.currentProxy(), 구조 분리)은 PART 13.3에서 다룬다.
 */
public class Example2_InternalCallFails {

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean CallService callService() { return new CallService(); }
        @Bean LogAspect logAspect() { return new LogAspect(); }
    }

    public static void main(String[] args) {
        System.out.println("== [내부 호출] this.external() -> 프록시 우회 -> 어드바이스 미적용 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        CallService bean = ctx.getBean(CallService.class);
        System.out.println("[bean.internalCaller() 호출] (내부에서 this.external() 호출)");
        bean.internalCaller();
        ctx.close();
        System.out.println("\n=> internalCaller 안의 this.external()에는 [AOP] 로그가 '안' 붙었다(예제1과 달리).");
        System.out.println("   내부 호출 this.x()는 진짜 객체에서 직접 일어나 프록시를 못 거치기 때문. 해결은 PART 13.3.");
    }
}
