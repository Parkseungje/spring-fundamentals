package com.study.part10_db.s05_jdbctemplate;

import com.study.part10_db.domain.Customer;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;

/**
 * [10.5] JdbcTemplate — 순수 JDBC의 '반복 코드'를 제거한다.
 *
 * 순수 JDBC의 고통(10.1~10.4에서 직접 겪음): 매 쿼리마다 Connection/PreparedStatement/ResultSet을 얻고,
 * try-with-resources로 닫고, SQLException을 처리하는 코드가 '본 로직(SQL+매핑)'보다 길었다. close를 빠뜨리면 누수.
 *
 * JdbcTemplate은 PART 8의 '템플릿 메소드 + 전략 패턴'의 실제 구현체다.
 *   - 변하지 않는 흐름(연결 획득/반환·Statement 생성·ResultSet 순회·예외 변환)은 JdbcTemplate이 '숨겨서' 처리.
 *   - 변하는 부분(SQL, 파라미터, 결과 매핑)만 개발자가 넘긴다.
 * 그래서 우리 코드에는 Connection/close/try-catch가 사라지고 'SQL + 파라미터 + RowMapper'만 남는다.
 *
 * RowMapper: ResultSet 한 행 -> 객체로 바꾸는 전략. 함수형 인터페이스라 람다로도 쓸 수 있다(PART 5).
 *   - BeanPropertyRowMapper: 컬럼명 <-> 필드명(snake_case <-> camelCase)을 자동 매핑해 주는 기본 구현.
 */
public class CustomerDao {

    private final JdbcTemplate jdbcTemplate;

    // JdbcTemplate은 DataSource만 있으면 만들 수 있다(10.3의 DataSource를 그대로 활용).
    public CustomerDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void createTable() {
        // execute: DDL 실행
        jdbcTemplate.execute("create table if not exists customers(" +
                "id varchar(50) primary key, name varchar(50), age int)");
    }

    public void add(Customer c) {
        // update: INSERT/UPDATE/DELETE (영향 행 수 반환). 파라미터는 ? 자리에 순서대로.
        jdbcTemplate.update("insert into customers(id, name, age) values (?, ?, ?)",
                c.getId(), c.getName(), c.getAge());
    }

    public Customer get(String id) {
        // queryForObject: 단일 행 -> 객체. 0건이면 EmptyResultDataAccessException, 2건+면 IncorrectResultSizeDataAccessException.
        // BeanPropertyRowMapper가 컬럼(name,age) -> 필드(name,age) 자동 매핑.
        return jdbcTemplate.queryForObject(
                "select id, name, age from customers where id = ?",
                new BeanPropertyRowMapper<>(Customer.class),
                id);
    }

    public List<Customer> findByMinAge(int minAge) {
        // query: 여러 행 -> List. 여기선 RowMapper를 '람다'로 직접 작성(ResultSet 한 행 -> Customer).
        RowMapper<Customer> mapper = (rs, rowNum) ->
                new Customer(rs.getString("id"), rs.getString("name"), rs.getInt("age"));
        return jdbcTemplate.query("select id, name, age from customers where age >= ? order by age", mapper, minAge);
    }

    public int count() {
        // queryForObject로 단일 값(개수)도 조회 가능.
        return jdbcTemplate.queryForObject("select count(*) from customers", Integer.class);
    }
}
