package com.study.part10_db.s01_jdbc_standard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * [10.1] JDBC '이전' vs '이후' 비교 — 표준 API가 없으면 DB마다 코드를 다시 짜야 한다.
 *
 * JDBC가 없던 시절엔 DB 벤더마다 '고유 API'가 달랐다. 연결·조회 메서드 이름조차 제각각이라, DB를 바꾸면
 * 코드를 통째로 다시 써야 했다. 아래 MySqlNativeApi / OracleNativeApi는 그 시절을 흉내 낸 '가짜' 클래스다
 * (실제 자바엔 없다 — 개념을 보여주기 위한 시뮬레이션). 메서드 이름이 서로 다른 점에 주목하라.
 *
 * JDBC '이후'엔 DriverManager/Connection/PreparedStatement/ResultSet이라는 '표준 API'로 통일되어, DB가
 * 바뀌어도 URL과 드라이버만 바꾸면 코드는 그대로다.
 */
public class BeforeAfterJdbc {

    // ===== JDBC '이전' 시뮬레이션: 벤더마다 API(메서드 이름)가 다르다 =====
    static class MySqlNativeApi {           // MySQL 전용 (가짜)
        void connectMySql(String host) { System.out.println("    MySQL 전용 connectMySql(" + host + ")"); }
        String runQuery(String sql)     { return "MySQL결과"; }     // 조회 메서드 이름: runQuery
    }
    static class OracleNativeApi {          // Oracle 전용 (가짜)
        void openOracle(String tns)  { System.out.println("    Oracle 전용 openOracle(" + tns + ")"); }
        String fetchRows(String sql) { return "Oracle결과"; }       // 조회 메서드 이름: fetchRows (다름!)
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== [JDBC 이전] DB마다 고유 API — 코드를 다시 짜야 함 =====");

        // MySQL을 쓰는 코드
        MySqlNativeApi mysql = new MySqlNativeApi();
        mysql.connectMySql("localhost");                 // connectMySql / runQuery
        System.out.println("    조회 = " + mysql.runQuery("select ..."));

        // DB를 Oracle로 바꾸면? -> 메서드 이름부터 달라서 '코드를 통째로 다시' 써야 한다.
        OracleNativeApi oracle = new OracleNativeApi();
        oracle.openOracle("orcl");                       // openOracle / fetchRows (이름이 다름!)
        System.out.println("    조회 = " + oracle.fetchRows("select ..."));
        System.out.println("    => 벤더마다 connect/open, runQuery/fetchRows... API가 달라 DB 교체 = 전면 재작성.");

        System.out.println("\n===== [JDBC 이후] 표준 API — DB가 바뀌어도 코드는 그대로(URL/드라이버만) =====");
        // 표준 JDBC: 어떤 DB든 DriverManager -> Connection -> PreparedStatement -> ResultSet 로 동일.
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:mem:part10_ba;DB_CLOSE_DELAY=-1";   // MySQL이면 jdbc:mysql://..., Oracle이면 jdbc:oracle:... 로 'URL만' 변경
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.prepareStatement("create table t(x int)").executeUpdate();
            c.prepareStatement("insert into t values (1)").executeUpdate();
            try (PreparedStatement ps = c.prepareStatement("select x from t");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("    표준 API 조회 결과 x = " + rs.getInt("x"));
            }
        }
        System.out.println("    => 연결=getConnection, 조회=executeQuery 로 '통일'. DB 교체 시 URL/드라이버만 바꾸면 끝.");
    }
}
