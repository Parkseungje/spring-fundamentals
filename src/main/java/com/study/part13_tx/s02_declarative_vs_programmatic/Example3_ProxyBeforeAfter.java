package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * [13.2] 예제3 — 프록시 '도입 전 vs 후': 트랜잭션 코드가 비즈니스에서 분리되는 과정.
 *
 * 프록시 도입 전(BeforeProxyService): PlatformTransactionManager로 getTransaction/commit/rollback을 직접
 * 호출하고 try-catch로 감싼다(PART 11.5의 수동 코드). 비즈니스 로직(이체)이 트랜잭션 기술 코드에 파묻힌다.
 *
 * 프록시 도입 후(AfterProxyService): @Transactional 한 줄. 트랜잭션 책임을 프록시가 전부 가져가고 본문엔
 * 순수 비즈니스만 남는다. 이게 가능한 건 PART 12의 빈 후처리기 + AnnotationAwareAspectJAutoProxyCreator 덕분.
 *
 * 두 서비스는 '같은 동작'(예외 시 롤백)을 하지만 코드 모습이 완전히 다르다. 같은 결과를 출력해 동작은
 * 같음을 확인하고, 소스에서 '코드량/관심사 분리'의 차이를 비교하는 게 이 예제의 목적이다.
 */
public class Example3_ProxyBeforeAfter {

    // ===== 프록시 도입 전: 트랜잭션 코드가 비즈니스에 뒤섞임 =====
    static class BeforeProxyService {
        private final AccountDao dao;
        private final PlatformTransactionManager txManager;
        BeforeProxyService(AccountDao dao, PlatformTransactionManager txManager) {
            this.dao = dao;
            this.txManager = txManager;
        }

        public void transfer(String from, String to, int amount) {
            // --- 트랜잭션 기술 코드(시작) ---
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());
            try {
                // --- 비즈니스 로직(이 한가운데에 파묻힘) ---
                dao.addBalance(from, -amount);
                if (amount > 0) {
                    throw new RuntimeException("입금 중 오류!");
                }
                dao.addBalance(to, amount);
                // --- 트랜잭션 기술 코드(커밋) ---
                txManager.commit(status);
            } catch (RuntimeException e) {
                txManager.rollback(status); // 직접 롤백
                throw e;
            }
        }
    }

    // ===== 프록시 도입 후: 순수 비즈니스만 =====
    static class AfterProxyService {
        private final AccountDao dao;
        AfterProxyService(AccountDao dao) { this.dao = dao; }

        @Transactional
        public void transfer(String from, String to, int amount) {
            dao.addBalance(from, -amount);
            if (amount > 0) {
                throw new RuntimeException("입금 중 오류!");
            }
            dao.addBalance(to, amount);
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean BeforeProxyService beforeProxyService(AccountDao dao, PlatformTransactionManager tm) {
            return new BeforeProxyService(dao, tm);
        }
        @Bean AfterProxyService afterProxyService(AccountDao dao) {
            return new AfterProxyService(dao);
        }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        AccountDao dao = ctx.getBean(AccountDao.class);

        System.out.println("== [프록시 도입 전] 수동 getTransaction/commit/rollback ==");
        dao.initSchema();
        BeforeProxyService before = ctx.getBean(BeforeProxyService.class);
        System.out.println("  실제 클래스 = " + before.getClass().getSimpleName() + " (프록시 아님 — 수동 트랜잭션)");
        runTransfer(dao, () -> before.transfer("A", "B", 300));

        System.out.println("\n== [프록시 도입 후] @Transactional ==");
        dao.initSchema();
        AfterProxyService after = ctx.getBean(AfterProxyService.class);
        System.out.println("  실제 클래스 = " + after.getClass().getSimpleName() + " (프록시)");
        runTransfer(dao, () -> after.transfer("A", "B", 300));

        ctx.close();
        System.out.println("\n=> 동작은 같다(둘 다 롤백 -> A=1000). 그러나 '전'은 트랜잭션 코드가 비즈니스에 뒤섞이고,");
        System.out.println("   '후'는 @Transactional 한 줄로 프록시가 책임을 가져가 본문이 순수 비즈니스만 남는다.");
    }

    private static void runTransfer(AccountDao dao, Runnable transfer) {
        System.out.println("  이체 전: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        try {
            transfer.run();
        } catch (RuntimeException e) {
            System.out.println("  예외 -> 롤백: " + e.getMessage());
        }
        System.out.println("  이체 후: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
    }
}
