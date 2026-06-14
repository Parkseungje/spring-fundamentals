package com.study.part10_db.s01_jdbc_standard;

import com.study.part10_db.domain.Customer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.1] JDBC 표준화 — "DB가 달라도 같은 API로 접근한다".
 *
 * JDBC 없던 시절의 고통: DB마다 고유 API(심하면 소켓으로 DB 바이너리 프로토콜을 직접 통신)였다.
 * DB를 바꾸면 모든 코드를 다시 짜야 했다.
 *
 * JDBC(Java Database Connectivity) = 자바의 'DB 접근 표준 API'. 연결·SQL 실행·결과 처리 방식을 통일한다.
 *   - 핵심 타입: Connection(연결), PreparedStatement(SQL+바인딩), ResultSet(결과).
 *   - DB를 바꿔도 'URL과 드라이버'만 바꾸면 나머지 코드는 그대로 — 인터페이스/전략 패턴 사상(PART 8.4).
 *
 * 이 데모는 'DB에 무관한 같은 코드(runWith)'를 서로 다른 두 연결(URL만 다름)에 대해 실행해, JDBC가
 * 표준 API라는 점을 보인다. (둘 다 H2지만 mem DB 이름을 달리해 '다른 DB'를 흉내 — 실무에선 MySQL/Oracle 등
 * 으로 바뀌어도 runWith 코드는 그대로고 URL/드라이버만 달라진다.)
 *
 * JDBC가 '해결 못 하는' 것: SQL '문법 차이'(예: 페이징 LIMIT vs ROWNUM, 자동증가 AUTO_INCREMENT vs SEQUENCE)
 * 는 그대로 남는다. 이 한계가 상위 추상화(JdbcTemplate/JPA/MyBatis)가 등장하는 동기다.
 */
public class JdbcStandardDemo {

    public static void main(String[] args) throws Exception {
        // 같은 코드(runWith)를, URL만 다른 두 연결에 대해 실행한다.
        System.out.println("== 연결 A (mem:part10_a) ==");
        runWith("jdbc:h2:mem:part10_a;DB_CLOSE_DELAY=-1");

        System.out.println("== 연결 B (mem:part10_b) ==");
        runWith("jdbc:h2:mem:part10_b;DB_CLOSE_DELAY=-1");

        System.out.println();
        System.out.println("=> 위 runWith()는 'URL' 말고는 한 줄도 안 다르다. DB가 바뀌어도 표준 JDBC API(Connection/");
        System.out.println("   PreparedStatement/ResultSet)는 그대로다 = JDBC 표준화의 힘(전략 패턴/DIP 사상).");
        System.out.println("   단, SQL 문법 차이(LIMIT vs ROWNUM 등)는 JDBC가 못 풀어준다 -> 상위 추상화의 동기(10.5~).");
    }

    // ★ DB에 무관한 '표준 JDBC' 코드. 바뀌는 건 인자로 받은 url(과 드라이버)뿐이다.
    static void runWith(String url) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");                          // 드라이버 (DB 바뀌면 이것과 url만 변경)
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.prepareStatement("create table if not exists customers(" +
                    "id varchar(50) primary key, name varchar(50), age int)").executeUpdate();

            // INSERT — 표준 PreparedStatement + 바인딩
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into customers(id, name, age) values (?, ?, ?)")) {
                ps.setString(1, "c1");
                ps.setString(2, "표준회원");
                ps.setInt(3, 30);
                ps.executeUpdate();
            }

            // SELECT — 표준 ResultSet
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, name, age from customers where id = ?")) {
                ps.setString(1, "c1");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Customer found = new Customer(rs.getString("id"), rs.getString("name"), rs.getInt("age"));
                    System.out.println("  조회 결과 = " + found);
                }
            }
        }
    }
}
