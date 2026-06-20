package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * [13.2] 트랜잭션 실습 공용 설정 — H2 인메모리 + 트랜잭션 매니저/템플릿.
 *
 * 13.2의 두 방식을 모두 보여주려면 인프라가 필요하다:
 *  - DataSource: H2 인메모리 DB 연결 공급(PART 10.3).
 *  - PlatformTransactionManager: 트랜잭션 시작/커밋/롤백을 추상화한 스프링 표준(PART 11.5).
 *      선언적(@Transactional)과 프로그래밍(TransactionTemplate) 둘 다 결국 이걸 사용한다.
 *  - TransactionTemplate: 프로그래밍 방식 트랜잭션용(콜백 안에서 커밋/롤백 자동 처리).
 *  - @EnableTransactionManagement: 선언적(@Transactional)을 켜는 스위치. 13.1에서 본 대로 내부적으로
 *      트랜잭션 AOP 설정을 @Import한다.
 *
 * 각 예제는 13.1의 @Import로 이 설정을 끌어와 재사용한다.
 */
@Configuration
@EnableTransactionManagement
public class TxConfig {

    @Bean
    public DataSource dataSource() {
        // 예제마다 별도 인메모리 DB를 쓰도록 이름을 고유하게(테스트 격리). DB_CLOSE_DELAY로 커넥션 닫혀도 유지.
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:tx_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
        return new TransactionTemplate(tm);
    }

    @Bean
    public AccountDao accountDao(JdbcTemplate jdbcTemplate) {
        return new AccountDao(jdbcTemplate);
    }
}
