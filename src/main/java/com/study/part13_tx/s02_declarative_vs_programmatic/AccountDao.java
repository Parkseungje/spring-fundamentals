package com.study.part13_tx.s02_declarative_vs_programmatic;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [13.2] 계좌 DAO — 잔액 조회/증감. 송금(이체) 시나리오로 트랜잭션의 원자성을 본다.
 * 트랜잭션 코드는 전혀 없다(순수 DB 연산). 트랜잭션 경계는 호출하는 서비스 쪽이 담당한다.
 */
public class AccountDao {

    private final JdbcTemplate jdbc;

    public AccountDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void initSchema() {
        jdbc.execute("create table if not exists account(id varchar(10) primary key, balance int)");
        jdbc.update("delete from account");
        jdbc.update("insert into account values ('A', 1000)");
        jdbc.update("insert into account values ('B', 1000)");
    }

    public int getBalance(String id) {
        return jdbc.queryForObject("select balance from account where id = ?", Integer.class, id);
    }

    public void addBalance(String id, int delta) {
        jdbc.update("update account set balance = balance + ? where id = ?", delta, id);
    }
}
