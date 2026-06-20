package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * [13.2] 예제1 — 프로그래밍 방식 트랜잭션(TransactionTemplate). 세밀 제어, 단 기술 코드가 비즈니스에 섞인다.
 *
 * TransactionTemplate.execute(콜백)는 콜백 안의 코드를 트랜잭션으로 감싼다. 콜백이 정상 종료하면 커밋,
 * 예외가 나거나 status.setRollbackOnly()면 롤백한다(12.3 템플릿 콜백 패턴의 실제 사례 — XxxTemplate).
 *
 * 장점: 트랜잭션 경계를 코드로 세밀하게 제어할 수 있다.
 * 단점(이 예제가 같이 보여줌): transactionTemplate.execute(...) 호출과 콜백 구조가 '비즈니스 로직'과
 * 뒤섞인다(기술 코드와 핵심 로직의 강결합). 메서드마다 이 보일러플레이트가 반복된다.
 *
 * 가설: 이체 도중 예외를 던지면 TransactionTemplate이 롤백하여 두 계좌 잔액이 원상복구된다(원자성).
 */
public class Example1_Programmatic {

    @Configuration
    @Import(TxConfig.class) // 13.1의 @Import로 공용 트랜잭션 설정 재사용
    static class Config {
    }

    public static void main(String[] args) {
        System.out.println("== [프로그래밍 방식] TransactionTemplate ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        AccountDao dao = ctx.getBean(AccountDao.class);
        TransactionTemplate txTemplate = ctx.getBean(TransactionTemplate.class);
        dao.initSchema();

        System.out.println("이체 전: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));

        // 비즈니스 로직(이체)과 트랜잭션 기술 코드(execute 콜백)가 한데 섞여 있다.
        try {
            txTemplate.execute(status -> {
                dao.addBalance("A", -300);  // A에서 300 출금
                System.out.println("  A 출금 완료(아직 커밋 전)");
                if (true) {
                    throw new RuntimeException("입금 중 오류! -> 롤백되어야 함");
                }
                dao.addBalance("B", 300);   // (도달 못 함) B에 입금
                return null;
            });
        } catch (RuntimeException e) {
            System.out.println("  예외: " + e.getMessage());
        }

        System.out.println("이체 후: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        ctx.close();
        System.out.println("\n=> 예외로 롤백되어 A 출금이 취소됐다(A=1000 유지). 원자성 OK.");
        System.out.println("   단 execute(콜백) 구조가 비즈니스 로직에 섞였다(기술-비즈니스 강결합) -> 선언적(예제2)이 이를 분리.");
    }
}
