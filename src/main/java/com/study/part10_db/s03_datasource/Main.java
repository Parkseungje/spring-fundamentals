package com.study.part10_db.s03_datasource;

import com.study.part10_db.domain.Customer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * [10.3 데모] 같은 CustomerDao에 서로 다른 DataSource 구현을 주입해도 그대로 동작한다.
 *
 * 핵심 관찰: dao 코드(add/get)는 한 줄도 안 바뀌고, 주입하는 DataSource 구현만 HikariDataSource ↔
 * DriverManagerDataSource로 바꾼다. "어떤 구현이든 getConnection()" 표준 덕분(DIP). 실무에선 HikariCP →
 * DBCP2 교체도 '설정만' 바꾸면 되고 애플리케이션 코드는 그대로다.
 *
 * 두 구현의 성격:
 *   - HikariDataSource: 진짜 커넥션 풀(재사용). 프로덕션용.
 *   - DriverManagerDataSource: DriverManager를 DataSource로 감싼 '어댑터'. 풀이 없어 매번 새 연결을 만든다.
 *     → 테스트·학습용. 프로덕션 금지(10.2의 비효율 그대로).
 */
public class Main {
    static final String URL = "jdbc:h2:mem:part10_ds;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        // (A) 구현 1: HikariCP 풀
        DataSource hikari = hikari();
        runWith("HikariDataSource(풀)", hikari, "h1");

        // (B) 구현 2: DriverManagerDataSource (풀 없음, 학습용)
        DataSource dmds = new DriverManagerDataSource(URL, "sa", "");
        runWith("DriverManagerDataSource(풀 없음)", dmds, "d1");

        ((HikariDataSource) hikari).close();

        System.out.println();
        System.out.println("=> CustomerDao 코드는 그대로, 주입하는 DataSource 구현만 교체했다. 'getConnection()' 표준(DIP) 덕분.");
        System.out.println("   실무: HikariCP -> DBCP2 교체도 설정만, 코드는 불변. DriverManagerDataSource는 학습용(프로덕션 금지).");
    }

    // 같은 DAO 코드를 '주입된 DataSource'로 실행 — 구현이 무엇이든 동일하게 동작.
    static void runWith(String label, DataSource ds, String id) throws Exception {
        CustomerDao dao = new CustomerDao(ds);   // DataSource 구현을 주입(둘 다 같은 DAO)
        dao.createTable();
        dao.add(new Customer(id, label, 30));
        System.out.println("[" + label + "] " + dao.get(id));
    }

    static DataSource hikari() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(URL);
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(5);
        return new HikariDataSource(cfg);
    }
}
