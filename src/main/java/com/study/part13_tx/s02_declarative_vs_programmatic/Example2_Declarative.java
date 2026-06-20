package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * [13.2] 예제2 — 선언적 방식 트랜잭션(@Transactional). 어노테이션 한 줄, 서비스엔 순수 비즈니스만.
 *
 * @Transactional을 메서드에 붙이면, 트랜잭션 시작/커밋/롤백을 '프록시'가 대신 처리한다(PART 12의 자동 프록시
 * 생성기 + 트랜잭션 어드바이저). 그래서 서비스 메서드 본문엔 트랜잭션 기술 코드 없이 '순수 비즈니스 로직'만 남는다.
 *
 * 예제1과의 차이: execute(콜백) 같은 기술 코드가 사라지고 transfer() 본문이 비즈니스(이체)만 담는다. 롤백도
 * 메서드에서 RuntimeException을 던지면 프록시가 알아서 처리한다(기본 롤백 정책 = 언체크 예외).
 *
 * 가설: transfer 도중 RuntimeException이 나면 @Transactional 프록시가 롤백해 잔액이 원상복구된다.
 */
public class Example2_Declarative {

    // 서비스: 트랜잭션 코드가 한 줄도 없다. @Transactional만 선언.
    static class TransferService {
        private final AccountDao dao;
        TransferService(AccountDao dao) { this.dao = dao; }

        @Transactional // 트랜잭션 경계는 프록시가 담당 -> 본문은 순수 비즈니스
        public void transfer(String from, String to, int amount) {
            dao.addBalance(from, -amount);            // 출금
            System.out.println("  " + from + " 출금 완료(아직 커밋 전)");
            if (amount > 0) {
                throw new RuntimeException("입금 중 오류! -> 프록시가 롤백");
            }
            dao.addBalance(to, amount);               // (도달 못 함) 입금
        }
    }

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean TransferService transferService(AccountDao dao) { return new TransferService(dao); }
    }

    public static void main(String[] args) {
        System.out.println("== [선언적 방식] @Transactional ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        AccountDao dao = ctx.getBean(AccountDao.class);
        TransferService service = ctx.getBean(TransferService.class);
        dao.initSchema();

        System.out.println("서비스 실제 클래스 = " + service.getClass().getName() + " (프록시)");
        System.out.println("이체 전: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));
        try {
            service.transfer("A", "B", 300);
        } catch (RuntimeException e) {
            System.out.println("  예외: " + e.getMessage());
        }
        System.out.println("이체 후: A=" + dao.getBalance("A") + " B=" + dao.getBalance("B"));

        ctx.close();
        System.out.println("\n=> @Transactional 프록시가 롤백해 A=1000 유지. 서비스 본문엔 트랜잭션 코드가 전혀 없다(순수 비즈니스).");
        System.out.println("   예제1(프로그래밍)의 execute 보일러플레이트가 사라졌다 -> 선언적이 거의 표준.");
    }
}
