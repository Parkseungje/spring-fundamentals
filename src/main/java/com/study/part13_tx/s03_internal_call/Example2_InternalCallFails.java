package com.study.part13_tx.s03_internal_call;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.3] 예제2 — ★★★ 함정: external()이 this.internal()을 부르면 @Transactional이 무시된다.
 *
 * external()은 @Transactional이 없다(트랜잭션 없이 시작). 그 안에서 this.internal()을 부르는데, this는
 * 프록시가 아니라 '진짜 객체(target)'다. 그래서 internal()의 @Transactional을 처리할 프록시를 거치지 않고
 * target의 internal()이 그냥 실행된다 -> 트랜잭션이 시작되지 않는다(isActualTransactionActive=false).
 *
 * 예제1과의 차이: internal()을 '외부에서 직접'(예제1, 프록시 경유 -> 적용) vs 'external 내부에서 this로'
 * (예제2, 프록시 우회 -> 무시). 같은 internal()인데 호출 경로 때문에 @Transactional 적용 여부가 갈린다.
 * 이것이 실무에서 "분명 @Transactional 붙였는데 롤백이 안 돼요"의 1순위 원인이다(면접 단골).
 */
public class Example2_InternalCallFails {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean TxService txService() { return new TxService(); }
    }

    public static void main(String[] args) {
        System.out.println("== [함정] external()이 this.internal() 호출 -> @Transactional 무시 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        TxService bean = ctx.getBean(TxService.class);
        bean.external(); // external -> this.internal() : 프록시 우회
        ctx.close();
        System.out.println("\n=> internal()에 @Transactional이 있어도 트랜잭션 활성=false! this 호출이 프록시를 우회했기 때문.");
        System.out.println("   (예제1은 true였다 — 차이는 '호출 경로'뿐.) 해결은 예제3(별도 빈 분리).");
    }
}
