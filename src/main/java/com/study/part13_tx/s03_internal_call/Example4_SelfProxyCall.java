package com.study.part13_tx.s03_internal_call;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * [13.3] 예제4 — 해결책(2): 내부 호출을 '현재 프록시'로 돌려 @Transactional을 적용시킨다.
 *
 * Example2는 this.internal()이 프록시를 우회해 트랜잭션이 안 걸렸다(false). 여기서는 AopContext.currentProxy()로
 * '프록시'를 얻어 proxy.internal()을 부른다 -> 프록시를 거치므로 @Transactional이 적용된다(true).
 *
 * 핵심 설정: @EnableAspectJAutoProxy(exposeProxy = true) — 프록시를 AopContext에 노출해야 currentProxy()가
 * 동작한다(이 옵션이 없으면 IllegalStateException).
 *
 * 가설: externalViaProxy() 내부에서 프록시 경유로 internal()을 부르면 트랜잭션 활성=true(Example2의 false와 대조).
 */
public class Example4_SelfProxyCall {

    @Configuration
    @Import(TxConfig.class)
    @EnableAspectJAutoProxy(exposeProxy = true)   // 프록시 노출(AopContext.currentProxy 사용 위해)
    static class Config {
        @Bean SelfProxyService selfProxyService() { return new SelfProxyService(); }
    }

    public static void main(String[] args) {
        System.out.println("== [해결책2] AopContext.currentProxy()로 내부 호출을 프록시 경유시키기 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        SelfProxyService bean = ctx.getBean(SelfProxyService.class);
        bean.externalViaProxy();
        ctx.close();
        System.out.println("\n=> this.internal()(Example2, false)과 달리, 프록시.internal()은 프록시를 거쳐 트랜잭션 적용=true.");
        System.out.println("   단 exposeProxy=true 설정 + 지저분한 코드가 필요 -> 가장 권장은 '별도 빈 분리'(Example3).");
    }
}
