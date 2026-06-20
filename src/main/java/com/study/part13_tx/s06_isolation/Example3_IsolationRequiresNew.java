package com.study.part13_tx.s06_isolation;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.6] 예제3 — 격리 x 전파: REQUIRES_NEW는 '새 트랜잭션'이라 자기 격리를 가진다.
 *
 * 예제2와 같은 구성인데 내부 전파만 REQUIRES_NEW로 바꿨다. REQUIRES_NEW는 외부를 보류하고 '새 물리
 * 트랜잭션'을 시작하므로(13.5), 그 시작 시점에 자기가 선언한 격리(SERIALIZABLE)가 그대로 적용된다.
 *
 * 가설: outer(READ_COMMITTED)가 inner(REQUIRES_NEW, 선언=SERIALIZABLE)를 부르면, inner의 실제 격리는
 * SERIALIZABLE로 찍힌다(예제2와 달리).
 *
 * 정리: "격리를 바꾸고 싶으면 새 트랜잭션을 시작해야 한다(REQUIRES_NEW). 기존 트랜잭션에 참여(REQUIRED)하면
 * 격리를 못 바꾼다." 이것이 13.5(전파)와 13.6(격리)이 만나는 지점이다.
 */
public class Example3_IsolationRequiresNew {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean IsoServices.InnerIso innerIso() { return new IsoServices.InnerIso(); }
        @Bean IsoServices.OuterIso outerIso(IsoServices.InnerIso inner) { return new IsoServices.OuterIso(inner); }
    }

    public static void main(String[] args) {
        System.out.println("== [격리 x 전파] REQUIRES_NEW -> 새 트랜잭션이라 자기 격리(SERIALIZABLE) 적용 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        IsoServices.OuterIso outer = ctx.getBean(IsoServices.OuterIso.class);

        outer.callNew();

        ctx.close();
        System.out.println("\n=> 같은 외부(READ_COMMITTED)인데 inner가 REQUIRES_NEW면 자기 격리(SERIALIZABLE)를 가진다.");
        System.out.println("   예제2(참여=외부 격리)와 대조. 격리를 바꾸려면 새 트랜잭션을 시작해야 한다.");
    }
}
