package com.study.part13_tx.s04_postconstruct;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.4] 예제2 — 해결: 초기화를 '컨테이너 완성 후' 시점으로 옮긴다(@EventListener + @Transactional).
 *
 * 함정(예제1)의 원인은 @PostConstruct가 '프록시 생성 이전'에 실행된다는 '타이밍'이었다. 그러니 실행 시점을
 * '모든 빈이 프록시로 교체까지 끝난 뒤'로 미루면 된다. 그 시점에 발생하는 이벤트를 받아 초기화하면, 그때는
 * 프록시가 이미 있으므로 @Transactional이 정상 적용된다.
 *
 *   - 여기서는 ContextRefreshedEvent(컨텍스트 초기화 완료 시 발행)를 쓴다. 이 시점엔 자동 프록시 생성기가
 *     모든 대상 빈을 프록시로 바꾼 뒤다 -> @Transactional 적용됨.
 *   - Spring Boot에서는 보통 ApplicationReadyEvent(애플리케이션 기동 완료)를 쓴다(같은 취지, 더 늦은 시점).
 *
 * 가설: @EventListener로 실행하면 트랜잭션 활성=true(예제1과 달리).
 */
public class Example2_EventListenerSolution {

    static class CacheLoader {
        // @PostConstruct 대신, 컨테이너가 완성된 뒤 발행되는 이벤트를 받아 초기화한다.
        @EventListener(ContextRefreshedEvent.class)
        @Transactional
        public void init() {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("    @EventListener(ContextRefreshed) init() -> 트랜잭션 활성? " + active
                    + (active ? "  (true = 적용됨! 프록시가 이미 준비된 시점)" : "  (false)"));
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean CacheLoader cacheLoader() { return new CacheLoader(); }
    }

    public static void main(String[] args) {
        System.out.println("== [해결1] @EventListener(ContextRefreshedEvent) + @Transactional ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        ctx.close();
        System.out.println("\n=> 컨테이너 완성 후 실행되어 프록시가 이미 있으므로 트랜잭션 활성=true.");
        System.out.println("   (Spring Boot에선 ApplicationReadyEvent를 같은 용도로 쓴다.)");
    }
}
