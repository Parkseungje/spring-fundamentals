package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.5] 예제3 — REQUIRES_NEW: 내부를 '별도 물리 트랜잭션'으로 분리(내부 롤백이 외부에 영향 X).
 *
 * 예제2의 함정(내부 실패가 외부까지 롤백시킴)을 풀려면, 내부를 외부와 '분리된 트랜잭션'으로 만들면 된다.
 * REQUIRES_NEW는 진행 중 외부 트랜잭션을 '잠시 보류'하고 새 물리 트랜잭션을 시작한다. 내부가 실패하면
 * 그 새 트랜잭션만 롤백되고, 외부가 그 예외를 잡으면 외부는 자기 작업을 그대로 커밋할 수 있다.
 *
 * 예제2와의 차이: 내부 전파만 REQUIRED -> REQUIRES_NEW로 바꿨다. 그 결과 외부(A-100)는 살아남고
 * 내부(B+100)만 롤백된다. (rollbackOnly가 외부 트랜잭션엔 안 묻기 때문.)
 *
 * 가설: A=900(외부 커밋), B=1000(내부만 롤백). UnexpectedRollbackException 없음.
 *
 * ⚠️ 주의: REQUIRES_NEW는 외부+내부가 '동시에 커넥션 2개'를 점유한다 -> 많이 쓰면 커넥션 풀 고갈 위험(10.2).
 */
public class Example3_RequiresNew {

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

        System.out.println("== REQUIRES_NEW: 내부는 별도 트랜잭션 -> 내부만 롤백, 외부는 커밋 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        outer.requiresNewInnerFailCaught(); // 내부 실패를 외부가 삼킴 -> 외부는 정상 커밋
        System.out.println("  결과: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B")
                + "  (A-100 외부 커밋 / B는 내부 별도 롤백 -> 1000 유지)");

        ctx.close();
        System.out.println("\n=> REQUIRES_NEW는 외부와 분리된 새 트랜잭션이라 내부 실패가 외부에 영향을 안 준다.");
        System.out.println("   예제2(REQUIRED, 전체 롤백+예외)와 대조된다. 단 커넥션 2개 동시 점유 주의.");
    }
}
