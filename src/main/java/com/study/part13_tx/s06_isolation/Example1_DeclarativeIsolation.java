package com.study.part13_tx.s06_isolation;

import com.study.part13_tx.s02_declarative_vs_programmatic.TxConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.6] 예제1 — 격리 수준을 '선언적으로' 지정한다(@Transactional(isolation=...)).
 *
 * 10.4에서는 conn.setTransactionIsolation()(JDBC 저수준)으로 격리를 바꿨다. Spring에서는 메서드에
 * @Transactional(isolation = Isolation.XXX)만 붙이면 그 트랜잭션이 그 격리 수준으로 시작된다(실무 표준).
 *  - 지정 안 하면 DEFAULT -> DB 기본 격리(H2는 READ COMMITTED)를 사용.
 *  - 명시하면 그 값이 현재 트랜잭션 격리로 잡힌다(getCurrentTransactionIsolationLevel로 확인).
 *
 * (이상현상 자체의 재현/매트릭스는 10.4 Example3_ReadPhenomena에서 이미 다뤘다. 여기선 'Spring 선언 방식'에 집중.)
 */
public class Example1_DeclarativeIsolation {

    @Configuration
    @Import(TxConfig.class)
    static class Config {
        @Bean IsoServices.SimpleIso simpleIso() { return new IsoServices.SimpleIso(); }
    }

    public static void main(String[] args) {
        System.out.println("== [선언적 격리 지정] @Transactional(isolation=...) ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        IsoServices.SimpleIso iso = ctx.getBean(IsoServices.SimpleIso.class);

        iso.useDefault();      // 지정 안 함 -> DB 기본
        iso.readCommitted();   // READ_COMMITTED(2)
        iso.repeatableRead();  // REPEATABLE_READ(4)

        ctx.close();
        System.out.println("\n=> @Transactional(isolation=...) 한 줄로 트랜잭션 격리 수준이 정해진다. 미지정이면 DB 기본.");
        System.out.println("   (이상현상 재현/격리 매트릭스는 10.4 참고. 여기선 Spring 선언 방식만 확인.)");
    }
}
