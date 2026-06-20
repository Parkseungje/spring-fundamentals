package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.5] 예제4 — NESTED: 외부와 '같은 트랜잭션 안의 Savepoint'로 부분 롤백(REQUIRES_NEW와 대조).
 *
 * 내부(NESTED)가 실패하고 외부가 예외를 잡으면, 내부 작업(B+100)은 Savepoint까지만 롤백되고 외부(A-100)는
 * 계속 진행해 커밋된다. 결과 잔액은 REQUIRES_NEW(예제3)와 같아 보이지만 메커니즘이 다르다:
 *   - REQUIRES_NEW: 외부와 '분리된 새 물리 트랜잭션'(커넥션 2개). 내부가 커밋하면 외부와 무관하게 확정.
 *   - NESTED: 외부와 '같은 물리 트랜잭션' 안의 Savepoint(커넥션 1개). 내부만 부분 롤백 가능하지만,
 *     외부가 나중에 롤백되면 내부도 함께 사라진다(외부에 종속). DataSourceTransactionManager가 Savepoint 지원.
 *
 * 가설: A=900(외부 커밋), B=1000(내부 Savepoint 롤백). UnexpectedRollbackException 없음.
 */
public class Example4_Nested {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean InnerService innerService(AccountDao dao) { return new InnerService(dao); }
        @Bean OuterService outerService(AccountDao dao, InnerService inner) { return new OuterService(dao, inner); }
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        AccountDao dao = ctx.getBean(AccountDao.class);
        OuterService outer = ctx.getBean(OuterService.class);

        System.out.println("== NESTED: 같은 트랜잭션 안 Savepoint로 부분 롤백 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        outer.nestedInnerFailCaught();
        System.out.println("  결과: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B")
                + "  (A-100 외부 커밋 / B는 Savepoint까지 롤백 -> 1000)");

        ctx.close();
        System.out.println("\n=> NESTED는 외부와 '같은 트랜잭션'의 Savepoint라 부분 롤백 후 외부는 계속 커밋된다.");
        System.out.println("   REQUIRES_NEW(별도 커넥션·완전 독립)와 결과는 비슷해도, NESTED는 커넥션 공유 + 외부 롤백 시 함께 롤백된다.");
    }
}
