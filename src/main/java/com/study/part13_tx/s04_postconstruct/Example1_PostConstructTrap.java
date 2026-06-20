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
 * [13.4] 예제1 — ★ 함정: @PostConstruct + @Transactional => 트랜잭션이 안 걸린다.
 *
 * @PostConstruct: 의존성 주입(DI)이 끝난 '직후' 자동 실행되는 초기화 메서드다. 생성자에선 아직 의존성이
 * 다 안 채워졌을 수 있어, "주입 완료 후 한 번 할 초기화"(캐시 예열 등)에 쓴다.
 *
 * 함정의 원인(타이밍):
 *   스프링이 빈을 만드는 순서는 대략 "객체 생성 -> 의존성 주입 -> @PostConstruct 실행 -> (빈 후처리기가)
 *   프록시로 교체 -> 컨테이너 등록"이다. 즉 @PostConstruct는 '프록시로 교체되기 이전'에 실행된다.
 *   그 시점엔 아직 프록시가 없으므로, @PostConstruct 안에서 @Transactional이 붙어 있어도 이를 처리할
 *   프록시가 없어 트랜잭션이 시작되지 않는다(13.3이 '공간상' 프록시 우회라면, 이건 '시간상' 프록시 이전).
 *
 * 가설: init()에 @Transactional을 붙여도, @PostConstruct로 실행되므로 isActualTransactionActive()=false.
 */
public class Example1_PostConstructTrap {

    static class CacheLoader {
        @PostConstruct
        @Transactional
        public void init() {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("    @PostConstruct init() 실행 -> 트랜잭션 활성? " + active
                    + (active ? "  (true)" : "  (false = @Transactional 무시됨! 아직 프록시가 없어서)"));
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean CacheLoader cacheLoader() { return new CacheLoader(); }
    }

    public static void main(String[] args) {
        System.out.println("== [함정] @PostConstruct + @Transactional ==");
        // 컨텍스트가 떠오르는 '도중' @PostConstruct가 실행된다(아래 줄에서 빈 생성 시점).
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        ctx.close();
        System.out.println("\n=> @PostConstruct는 '프록시 생성 이전'에 실행되어, @Transactional이 있어도 트랜잭션이 안 걸린다(false).");
        System.out.println("   해결: 컨테이너가 '완성된 뒤' 실행되는 시점으로 옮긴다(예제2) 또는 별도 빈 호출(예제3).");
    }
}
