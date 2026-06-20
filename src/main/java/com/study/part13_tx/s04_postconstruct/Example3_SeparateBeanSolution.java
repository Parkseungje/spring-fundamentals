package com.study.part13_tx.s04_postconstruct;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.4] 예제3 — 해결: @PostConstruct는 두되, 트랜잭션 작업은 '별도 빈'에 맡겨 호출한다.
 *
 * @PostConstruct 자체를 쓰고 싶다면, 그 안에서 @Transactional을 직접 붙이지 말고 '이미 프록시로 만들어진
 * 다른 빈'의 @Transactional 메서드를 호출하면 된다. @PostConstruct 시점에 '자기 자신'은 아직 프록시가
 * 아니지만, 주입받은 '다른 빈'은 이미 프록시로 완성돼 있으므로 그 빈을 거치면 트랜잭션이 적용된다.
 *
 *   - 주의: 주입받는 다른 빈이 '내 @PostConstruct 시점에 이미 프록시로 준비돼 있어야' 한다. 스프링은 의존
 *     빈을 먼저 생성·초기화(프록시 포함)한 뒤 나를 만들므로, 보통 주입된 의존 빈은 이미 프록시 상태다.
 *
 * 13.3(예제3)과 같은 처방(별도 빈 분리)이다 — 결국 "프록시를 거치게 만든다"는 동일한 해법.
 *
 * 가설: @PostConstruct에서 별도 빈(InnerService 프록시)의 메서드를 부르면 트랜잭션 활성=true.
 */
public class Example3_SeparateBeanSolution {

    static class InnerService {
        @Transactional
        public void load() {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("    InnerService.load() -> 트랜잭션 활성? " + active
                    + (active ? "  (true = 적용됨!)" : "  (false)"));
        }
    }

    static class CacheLoader {
        private final InnerService innerService; // 이미 프록시로 완성된 다른 빈을 주입
        CacheLoader(InnerService innerService) { this.innerService = innerService; }

        @PostConstruct
        public void init() {
            System.out.println("    @PostConstruct init() -> innerService.load() 호출(다른 빈=프록시):");
            innerService.load(); // 다른 빈(프록시)을 거침 -> @Transactional 적용
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean InnerService innerService() { return new InnerService(); }
        @Bean CacheLoader cacheLoader(InnerService innerService) { return new CacheLoader(innerService); }
    }

    public static void main(String[] args) {
        System.out.println("== [해결2] @PostConstruct에서 별도 빈(프록시)의 @Transactional 호출 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        ctx.close();
        System.out.println("\n=> 자기 @PostConstruct는 프록시 이전이지만, 주입된 '다른 빈'은 이미 프록시라 호출 시 트랜잭션 적용=true.");
        System.out.println("   13.3과 같은 처방(별도 빈 분리 = 프록시를 거치게 한다).");
    }
}
