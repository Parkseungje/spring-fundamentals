package com.study.part10_db.s02_connection_pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [10.2] Connection Pool — "매 요청마다 새 연결"의 비효율을 '연결 재사용'으로 푼다.
 *
 * 로우레벨의 비효율: DB 연결 1회 = TCP 3-way handshake + 인증 + 세션 생성. 실제 DB(MySQL/Oracle)에선
 * 수~수백 ms가 걸려, 매 요청마다 연결을 새로 만들면 '연결 설정'이 '쿼리 실행'보다 더 오래 걸리기도 한다.
 *
 * Connection Pool('커넥션의 수영장'): 시작 시 연결 N개를 미리 만들어 두고, 요청이 오면 빌려주고(borrow),
 * 다 쓰면 반환(return)받아 '재사용'한다. 매번 만들지 않으니 빠르고 자원이 일정하다.
 *   - 대표 구현: HikariCP(Spring Boot 기본), DBCP2, Tomcat JDBC Pool.
 *   - 풀의 Connection N개 = DB 세션 N개. (한 Connection에서 동시에 두 트랜잭션은 불가 — 10.4와 연결)
 *
 * 이 데모는 같은 작업(연결 얻기 -> 가벼운 쿼리 -> 반납)을 (A) 매번 DriverManager로 새 연결 / (B) HikariCP
 * 풀에서 빌려 재사용 으로 N번 반복해 시간을 비교한다.
 * 주의: 여기선 H2 '인메모리'라 연결 비용이 원래 매우 싸서 차이가 작거나 역전될 수도 있다. 실제 원격 DB일수록
 * (핸드셰이크·인증 비용이 커서) 풀의 이득이 폭발적으로 커진다 — 그 점을 염두에 두고 결과를 본다.
 */
public class ConnectionPoolDemo {

    static final String URL = "jdbc:h2:mem:part10_pool;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        // 표 준비(한 번)
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.prepareStatement("create table if not exists t(x int)").executeUpdate();
        }

        int n = 2000;

        // (A) 매번 새 연결: getConnection 자체가 매 반복마다 연결 생성 비용을 치른다.
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
                c.prepareStatement("select 1").executeQuery().close();
            }
        }
        long noPool = System.currentTimeMillis() - t1;

        // (B) 풀에서 빌려 재사용: 미리 만든 연결을 borrow/return 한다(생성 비용 없음).
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);   // 연결 10개를 만들어 재사용
        try (HikariDataSource pool = new HikariDataSource(config)) {
            long t2 = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                try (Connection c = pool.getConnection()) {   // 빌림 -> try 종료 시 반납(close=반환)
                    c.prepareStatement("select 1").executeQuery().close();
                }
            }
            long withPool = System.currentTimeMillis() - t2;

            System.out.println("[10.2] 연결 얻기+가벼운 쿼리 " + n + "회 반복");
            System.out.println("  (A) 매번 새 연결(DriverManager) : " + noPool + " ms");
            System.out.println("  (B) 풀에서 재사용(HikariCP)     : " + withPool + " ms  (풀 크기 10, 연결 재사용)");
            System.out.println("  => 풀은 연결을 미리 만들어 재사용하므로 '연결 생성 비용'을 매번 안 치른다.");
            System.out.println("     H2 인메모리라 차이가 작을 수 있으나, 원격 DB(핸드셰이크·인증)일수록 풀의 이득이 커진다.");
        }
    }
}
