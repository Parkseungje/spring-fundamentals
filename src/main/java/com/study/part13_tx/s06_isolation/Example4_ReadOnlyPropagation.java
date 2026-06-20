package com.study.part13_tx.s06_isolation;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.6] 예제4 — readOnly도 격리와 '같은 규칙': 트랜잭션 시작 시점 속성이라 참여하면 외부를 따른다.
 *
 * 1-3에서 본 "격리는 트랜잭션 시작 시 정해져 REQUIRED 참여하면 못 바꾼다"는 격리만의 특성이 아니다.
 * readOnly·timeout 같은 '트랜잭션 시작 시점 속성'은 전부 같은 규칙이다. 여기서는 readOnly로 확인한다:
 *   - 외부(쓰기 트랜잭션)에 inner(readOnly=true)가 REQUIRED로 참여 -> 외부의 readOnly(false)를 따름(선언 무시)
 *   - inner가 REQUIRES_NEW면 새 트랜잭션이라 자기 readOnly(true) 적용
 * 관찰: TransactionSynchronizationManager.isCurrentTransactionReadOnly()
 *
 * 예제2·3(격리)과 완전히 같은 패턴이 readOnly에서도 재현된다 -> "참여 시 외부 따름 / 새 트랜잭션이면 자기 것"은
 * 트랜잭션 시작 속성 전반의 일반 규칙임을 보여준다.
 */
public class Example4_ReadOnlyPropagation {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean IsoServices.ReadOnlyInner readOnlyInner() { return new IsoServices.ReadOnlyInner(); }
        @Bean IsoServices.ReadOnlyOuter readOnlyOuter(IsoServices.ReadOnlyInner inner) {
            return new IsoServices.ReadOnlyOuter(inner);
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        IsoServices.ReadOnlyOuter outer = ctx.getBean(IsoServices.ReadOnlyOuter.class);

        System.out.println("== (A) REQUIRED 참여 -> 내부 readOnly 무시(외부 따름) ==");
        outer.callParticipating();

        System.out.println("\n== (B) REQUIRES_NEW -> 내부 readOnly(true) 적용 ==");
        outer.callNew();

        ctx.close();
        System.out.println("\n=> 격리와 똑같이, readOnly도 참여(REQUIRED)면 외부를 따르고 새 트랜잭션(REQUIRES_NEW)이면 자기 것이 적용된다.");
        System.out.println("   즉 '트랜잭션 시작 시점 속성(격리/readOnly/timeout)'은 전부 같은 규칙을 따른다.");
    }
}
