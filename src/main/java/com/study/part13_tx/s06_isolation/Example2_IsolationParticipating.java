package com.study.part13_tx.s06_isolation;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.6] 예제2 — ★ 격리 x 전파: REQUIRED로 '참여'하면 내부가 선언한 격리는 무시된다.
 *
 * 격리 수준은 "트랜잭션이 '시작될 때'" 정해진다. 그래서 내부가 REQUIRED로 '이미 진행 중인 외부 트랜잭션에
 * 참여'하면, 새 트랜잭션을 시작하는 게 아니라 외부 것을 그대로 쓰므로 내부가 선언한 격리(SERIALIZABLE)는
 * 적용될 자리가 없다 -> 외부 격리(READ_COMMITTED)를 따른다.
 *
 * 가설: outer(READ_COMMITTED)가 inner(REQUIRED, 선언=SERIALIZABLE)를 부르면, inner의 실제 격리는
 * SERIALIZABLE이 아니라 READ_COMMITTED로 찍힌다.
 *
 * (참고: 스프링은 기본적으로 참여 트랜잭션의 격리 불일치를 검증하지 않아 '조용히 무시'한다. transactionManager의
 * validateExistingTransaction=true로 켜면 IllegalTransactionStateException으로 막을 수도 있다.)
 */
public class Example2_IsolationParticipating {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean IsoServices.InnerIso innerIso() { return new IsoServices.InnerIso(); }
        @Bean IsoServices.OuterIso outerIso(IsoServices.InnerIso inner) { return new IsoServices.OuterIso(inner); }
    }

    public static void main(String[] args) {
        System.out.println("== [격리 x 전파] REQUIRED 참여 -> 내부 선언 격리 무시(외부 따름) ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        IsoServices.OuterIso outer = ctx.getBean(IsoServices.OuterIso.class);

        outer.callParticipating();

        ctx.close();
        System.out.println("\n=> inner가 SERIALIZABLE을 선언했어도, 외부에 '참여'하면 외부 격리(READ_COMMITTED)를 따른다.");
        System.out.println("   격리는 '트랜잭션 시작 시점'에 정해지기 때문. 새 트랜잭션이 아니면 못 바꾼다. -> 예제3과 대조.");
    }
}
