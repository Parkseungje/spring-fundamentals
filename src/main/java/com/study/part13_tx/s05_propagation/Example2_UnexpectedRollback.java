package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * [13.5] 예제2 — ★ 함정: 내부(REQUIRED) 실패를 외부가 try-catch로 삼켜도 커밋이 안 되고 예외가 터진다.
 *
 * "내부 예외를 잡았으니 외부는 계속 진행하면 되겠지?"라고 생각하기 쉽다. 그러나 REQUIRED 내부가 실패하면,
 * 같은 물리 트랜잭션을 '롤백해야 함(rollbackOnly)'으로 표시해 버린다. 외부가 예외를 삼키고 정상 종료해도,
 * 커밋 시점에 프록시가 rollbackOnly를 발견하고 -> 실제로는 롤백하면서 UnexpectedRollbackException을 던진다.
 *
 * 본질: "논리 트랜잭션(내부)이 하나라도 롤백 표시되면, 물리 트랜잭션(전체)은 롤백된다." 내부 commit/rollback은
 * '논리적'일 뿐, 실제 물리 commit/rollback은 외부(트랜잭션을 시작한 쪽)에서 일어난다.
 *
 * 예제1(B)과의 차이: 거기선 예외를 안 잡아 그대로 롤백됐다. 여기선 '잡았는데도' 결국 롤백+예외가 난다 -
 * 이게 더 헷갈리는 함정이다.
 *
 * 가설: requiredInnerFailCaught() 호출 시 UnexpectedRollbackException 발생, A·B 둘 다 롤백.
 */
public class Example2_UnexpectedRollback {

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

        System.out.println("== [함정] 내부(REQUIRED) 실패를 외부가 catch로 삼킨 뒤 커밋 시도 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        try {
            outer.requiredInnerFailCaught();
            System.out.println("  (도달하면 안 됨) 정상 커밋됨");
        } catch (UnexpectedRollbackException e) {
            System.out.println("  -> UnexpectedRollbackException 발생! 메시지: " + e.getMessage());
        }
        System.out.println("  결과: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B")
                + "  (둘 다 롤백 = 내부가 rollbackOnly 표시했기 때문)");

        ctx.close();
        System.out.println("\n=> 내부 예외를 '잡아도' 소용없다. REQUIRED 내부 실패는 물리 트랜잭션을 rollbackOnly로 표시하고,");
        System.out.println("   외부 커밋 시점에 그것이 발견되어 롤백 + UnexpectedRollbackException이 난다.");
        System.out.println("   (외부가 끝까지 살리고 싶으면 내부를 REQUIRES_NEW로 분리해야 한다 -> 예제3.)");
    }
}
