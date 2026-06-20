package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * [13.2] 예제4 — 프로그래밍 방식만의 능력: setRollbackOnly()로 '예외 없이' 롤백한다.
 *
 * 선언적(@Transactional)은 '예외를 던져야' 롤백된다. 그런데 "예외를 던지진 않지만(정상 흐름 유지) 이 작업은
 * 취소하고 싶다"는 경우가 있다(검증 실패, 비즈니스 규칙 위반 등). 프로그래밍 방식은 콜백에서
 * status.setRollbackOnly()로 '롤백 예약'만 해두면, 예외 없이도 트랜잭션이 롤백된다.
 *
 * 이 예제는 같은 출금 작업을 두 번 한다:
 *   (A) setRollbackOnly() 호출 -> 예외 없이 정상 종료했지만 롤백됨(A 원복)
 *   (B) 아무 표시 없이 정상 종료 -> 커밋됨(A 반영)
 * 이것이 "프로그래밍 방식을 굳이 왜 쓰나"의 실제 답 중 하나다(세밀한 조건부 롤백).
 *
 * (참고: TransactionTemplate도 자동 롤백은 '언체크 예외'만 — @Transactional과 같은 정책. 예외를 안 쓰고
 *  롤백하려면 이 setRollbackOnly를 쓴다.)
 */
public class Example4_SetRollbackOnly {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        AccountDao dao = ctx.getBean(AccountDao.class);
        TransactionTemplate txTemplate = ctx.getBean(TransactionTemplate.class);

        System.out.println("== (A) setRollbackOnly() — 예외 없이 롤백 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A"));
        txTemplate.execute(status -> {
            dao.addBalance("A", -300);           // 출금
            System.out.println("  A에서 300 출금 후, 검증 실패라 가정 -> setRollbackOnly()");
            status.setRollbackOnly();            // 예외를 던지지 않고 '롤백 예약'
            return null;                         // 정상 종료(예외 없음)지만, rollbackOnly라 롤백된다
        });
        System.out.println("  결과: A=" + dao.getBalance("A") + "   <- 예외 없이도 롤백됨(1000 원복)\n");

        System.out.println("== (B) 표시 없음 — 정상 커밋 ==");
        dao.initSchema();
        System.out.println("  시작: A=" + dao.getBalance("A"));
        txTemplate.execute(status -> {
            dao.addBalance("A", -300);
            System.out.println("  A에서 300 출금, 롤백 표시 없음 -> 커밋");
            return null;
        });
        System.out.println("  결과: A=" + dao.getBalance("A") + "   <- 커밋됨(700 반영)");

        ctx.close();
        System.out.println("\n=> setRollbackOnly()는 '예외를 던지지 않고' 롤백하게 한다(프로그래밍 방식만의 세밀 제어).");
        System.out.println("   선언적(@Transactional)은 예외를 던져야 롤백된다는 점과 대비된다.");
    }
}
