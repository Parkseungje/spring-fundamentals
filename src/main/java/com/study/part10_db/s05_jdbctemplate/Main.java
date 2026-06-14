package com.study.part10_db.s05_jdbctemplate;

import com.study.part10_db.domain.Customer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.List;

/**
 * [10.5 데모] JdbcTemplate으로 작성하면 Connection/close/try-catch가 사라지고 SQL+매핑만 남는다.
 *
 * 10.1~10.4의 순수 JDBC 코드와 CustomerDao를 비교해 보면, 여기엔 try-with-resources도, ResultSet 순회도,
 * SQLException 처리도 없다. JdbcTemplate이 그 반복(템플릿)을 다 숨겨주기 때문이다.
 */
public class Main {
    public static void main(String[] args) {
        DataSource ds = hikari();
        CustomerDao dao = new CustomerDao(ds);

        dao.createTable();
        dao.add(new Customer("c1", "김삼십", 30));
        dao.add(new Customer("c2", "이사십", 40));
        dao.add(new Customer("c3", "박이십", 20));

        System.out.println("[10.5] count = " + dao.count());
        System.out.println("[10.5] get(c1) = " + dao.get("c1") + "  (BeanPropertyRowMapper 자동 매핑)");

        List<Customer> over30 = dao.findByMinAge(30);
        System.out.println("[10.5] age>=30 = " + over30 + "  (RowMapper 람다)");

        System.out.println("=> Connection/close/try-catch가 코드에서 사라지고 'SQL + 파라미터 + RowMapper'만 남았다.");
        System.out.println("   JdbcTemplate = 관심사 분리 + 템플릿 메소드 + 전략 패턴 + DI + 람다의 집약체(PART 8 원칙의 실제 구현).");

        ((HikariDataSource) ds).close();
    }

    static DataSource hikari() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:part10_jt;DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        return new HikariDataSource(cfg);
    }
}
