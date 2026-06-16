package com.study.part10_db.s01_jdbc_standard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * [10.1] JDBC가 '해결 못 하는' 것 — SQL 문법 차이(방언, dialect)는 그대로 남는다.
 *
 * JDBC는 'API'(Connection/PreparedStatement/ResultSet)는 통일했지만, 'SQL 문법'까지 통일하진 못한다.
 * 같은 의도("상위 N건만", "PK 자동 증가")라도 DB마다 SQL 문장이 다르다. 그래서 DB를 바꾸면 API 코드는
 * 그대로여도 'SQL 문자열'은 손봐야 한다. 이 한계가 JdbcTemplate/JPA 같은 상위 추상화가 등장하는 동기다.
 *
 * 아래는 DB별로 '다른 SQL'을 상수로 나란히 두어 차이를 코드로 표시한 것이다.
 */
public class DialectDifference {

    // ── 페이징(상위 N건): "직원을 2건만" ──
    static final String PAGING_MYSQL  = "select * from emp order by id LIMIT 2";          // MySQL/H2/PostgreSQL
    static final String PAGING_ORACLE = "select * from emp where ROWNUM <= 2 order by id"; // Oracle(구버전)
    // (※ 표준 SQL은 FETCH FIRST 2 ROWS ONLY 지만, 현업엔 LIMIT/ROWNUM 혼재)

    // ── 자동 증가 PK: 테이블 정의 ──
    static final String AUTOINC_MYSQL  = "create table emp(id BIGINT AUTO_INCREMENT primary key, name varchar(20))"; // MySQL
    static final String AUTOINC_ORACLE = "create sequence emp_seq; -- 그리고 insert 시 emp_seq.NEXTVAL 사용";          // Oracle

    public static void main(String[] args) throws Exception {
        System.out.println("== 같은 의도, 다른 SQL (JDBC는 이 차이를 못 없앤다) ==");
        System.out.println("[페이징: 상위 2건]");
        System.out.println("  MySQL/H2 : " + PAGING_MYSQL);
        System.out.println("  Oracle   : " + PAGING_ORACLE + "   <- LIMIT 대신 ROWNUM (문법 다름!)");
        System.out.println("[자동 증가 PK]");
        System.out.println("  MySQL    : " + AUTOINC_MYSQL);
        System.out.println("  Oracle   : " + AUTOINC_ORACLE + "   <- AUTO_INCREMENT 대신 SEQUENCE (문법 다름!)");

        // 실제로 H2에서 'MySQL식 LIMIT'은 동작하지만, 'Oracle식'은 그대로면 문법/동작이 달라 호환 안 됨을 확인.
        System.out.println("\n== H2에서 실행 (H2는 LIMIT 지원) ==");
        Class.forName("org.h2.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:part10_dialect;DB_CLOSE_DELAY=-1", "sa", "");
             Statement st = c.createStatement()) {
            st.execute("create table emp(id int primary key, name varchar(20))");
            st.execute("insert into emp values (1,'A'),(2,'B'),(3,'C')");

            System.out.println("  MySQL/H2식 LIMIT 실행 -> 상위 2건:");
            try (ResultSet rs = st.executeQuery(PAGING_MYSQL)) {
                while (rs.next()) System.out.println("    " + rs.getInt("id") + " " + rs.getString("name"));
            }
            System.out.println("  => 같은 'API'(executeQuery)로 실행했지만, 이 SQL 문장 자체는 Oracle에선 안 통한다(ROWNUM 필요).");
        }

        System.out.println("\n=> JDBC는 'API'는 통일하지만 'SQL 문법(방언)'은 통일 못 한다. DB 교체 시 SQL 문자열을 손봐야 한다.");
        System.out.println("   이 방언 차이를 흡수하려는 것이 JdbcTemplate/JPA/MyBatis 같은 상위 추상화다(10.5, PART 11).");
    }
}
