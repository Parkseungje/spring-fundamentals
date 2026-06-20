package com.study.part13_tx.s03_internal_call;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.3] 예제3 — 해결: @Transactional 메서드를 '별도 빈'으로 분리한다(권장).
 *
 * 함정의 원인(예제2)은 this.internal() = 같은 객체 내부 호출이라 프록시를 못 거치는 것이었다. 해결은
 * @Transactional 메서드를 '다른 빈'으로 옮기는 것이다. 다른 빈을 호출하면 그 빈의 '프록시'를 거치게 되어
 * @Transactional이 정상 적용된다.
 *
 * 구조: OuterService(트랜잭션 없음)가 InnerService(주입받은 별도 빈)의 @Transactional 메서드를 호출.
 *  -> innerService는 프록시라, innerService.doWork() 호출이 프록시를 거쳐 트랜잭션이 시작된다(active=true).
 *
 * 왜 권장인가: 단순히 함정을 피하는 것을 넘어, '트랜잭션 단위 작업'을 독립된 책임(빈)으로 분리하는 것은
 * SRP(단일 책임) 관점에서도 더 깔끔하다. (다른 해결책: AopContext.currentProxy()로 자기 프록시 호출,
 * 자기 자신 주입(self-injection) — 문서 참고. 그러나 구조 분리가 가장 권장된다. AspectJ 컴파일 방식이면
 * 애초에 이 문제가 없다 — 12.11.)
 *
 * 예제2와의 차이: internal()을 '같은 클래스'에 두지 않고 '다른 빈'으로 빼서, 호출이 프록시를 거치게 만든다.
 */
public class Example3_SeparateBean {

    // @Transactional 메서드를 가진 '별도 빈'
    static class InnerService {
        @Transactional
        public void doWork() {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("    InnerService.doWork() -> 트랜잭션 활성? " + active
                    + (active ? "  (true = 적용됨!)" : "  (false)"));
        }
    }

    // 트랜잭션 없는 바깥 서비스. 별도 빈(InnerService)을 주입받아 호출 -> 프록시를 거침
    static class OuterService {
        private final InnerService innerService; // 다른 빈(프록시)을 주입
        OuterService(InnerService innerService) { this.innerService = innerService; }

        public void run() {
            System.out.println("    OuterService.run() -> innerService.doWork() 호출(다른 빈=프록시 경유):");
            innerService.doWork(); // 다른 빈의 메서드 -> 프록시를 거쳐 @Transactional 적용
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean InnerService innerService() { return new InnerService(); }
        @Bean OuterService outerService(InnerService innerService) { return new OuterService(innerService); }
    }

    public static void main(String[] args) {
        System.out.println("== [해결] @Transactional 메서드를 별도 빈으로 분리 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        OuterService outer = ctx.getBean(OuterService.class);
        outer.run();
        ctx.close();
        System.out.println("\n=> 다른 빈(InnerService 프록시)을 거쳐 호출하니 트랜잭션 활성=true. 함정 해결.");
        System.out.println("   같은 클래스 this 호출(예제2)과 달리, 다른 빈 호출은 프록시를 거치기 때문.");
    }
}
