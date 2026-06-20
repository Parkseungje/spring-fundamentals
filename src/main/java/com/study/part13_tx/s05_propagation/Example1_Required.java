package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.5] 예제1 — REQUIRED(기본): 외부+내부가 '하나의 물리 트랜잭션'으로 묶인다.
 *
 * (A) 내부 정상: A-100(외부), B+100(내부)이 함께 커밋된다.
 * (B) 내부 실패(예외 전파): 같은 물리 트랜잭션이므로 외부 A-100까지 '전부' 롤백된다.
 *     -> "내부가 새 트랜잭션이 아니라 외부에 참여했다"는 증거(내부 실패가 외부 작업까지 되돌림).
 *
 * 가설: (A) A=900,B=1100 (둘 다 반영) / (B) A=1000,B=1000 (둘 다 롤백).
 */
public class Example1_Required {

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

        System.out.println("== (A) REQUIRED + 내부 정상: 하나로 묶여 함께 커밋 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        outer.requiredInnerOk();
        System.out.println("  결과: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B") + "  (A-100,B+100 함께 반영)\n");

        System.out.println("== (B) REQUIRED + 내부 실패(전파): 외부까지 전부 롤백 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        try {
            outer.requiredInnerFailNotCaught();
        } catch (RuntimeException e) {
            System.out.println("  예외 전파: " + e.getMessage());
        }
        System.out.println("  결과: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B")
                + "  (둘 다 원복 = 하나의 물리 트랜잭션이라 내부 실패가 외부까지 롤백)");

        ctx.close();
        System.out.println("\n=> REQUIRED는 외부에 '참여'한다. 그래서 내부 실패가 외부 작업까지 함께 롤백시킨다(한 물리 트랜잭션).");
    }
}
