package com.study.part13_tx.s03_internal_call;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.3] 예제1 — 기준선: @Transactional internal()을 '외부에서(프록시로)' 직접 부르면 트랜잭션이 걸린다.
 *
 * 클라이언트가 bean.internal()을 직접 호출하면 그 호출은 프록시를 거치므로 트랜잭션 어드바이저가 작동한다.
 * -> internal() 안에서 isActualTransactionActive()가 true. 이것이 '정상 동작' 기준이며, 예제2(내부 호출)와
 * 대조된다.
 */
public class Example1_ExternalCallWorks {

    @Configuration
    @Import(TxConfig.class) // 13.2의 트랜잭션 설정(txManager, @EnableTransactionManagement) 재사용
    static class Config {
        @Bean TxService txService() { return new TxService(); }
    }

    public static void main(String[] args) {
        System.out.println("== [기준선] 외부에서 internal() 직접 호출(프록시 경유) ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        TxService bean = ctx.getBean(TxService.class);
        System.out.println("빈 실제 클래스 = " + bean.getClass().getSimpleName() + " (프록시)");
        bean.internal(); // 프록시를 통한 외부 호출 -> @Transactional 적용
        ctx.close();
        System.out.println("\n=> 외부에서 프록시로 internal()을 부르니 트랜잭션 활성=true. 정상. 예제2와 비교하라.");
    }
}
