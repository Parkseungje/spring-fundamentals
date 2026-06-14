package com.study.part10_db.s04_transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.4 - 예시2] 격리성(Isolation) — "commit 전 변경은 다른 세션에 안 보인다".
 *
 * 두 개의 연결(세션)을 동시에 열고, 세션1이 값을 바꾸되 'commit하지 않은' 상태에서 세션2가 그 값을 읽는다.
 * 격리성 덕분에 세션2는 '변경 전(옛) 값'을 본다. 세션1이 commit한 뒤에야 세션2도 새 값을 본다.
 *   - H2 기본 격리 수준은 READ COMMITTED: "커밋된 데이터만 읽는다" -> 미커밋 변경(dirty)을 안 본다.
 *   - 비유: 각자 노트북에서 작업(미커밋) -> 클라우드에 저장(commit)해야 남이 본다.
 *
 * 격리 수준(낮을수록 빠르지만 위험): READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE.
 *   - READ UNCOMMITTED면 미커밋 값을 보는 'Dirty Read'가 생긴다(이 데모의 반대 상황). 보통은 피한다.
 */
public class Example2_CommitVisibility {

    static final String URL = "jdbc:h2:mem:part10_tx2;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        setUp();

        // 세션1, 세션2를 동시에 연다(서로 다른 Connection = 서로 다른 DB 세션).
        try (Connection s1 = DriverManager.getConnection(URL, "sa", "");
             Connection s2 = DriverManager.getConnection(URL, "sa", "")) {

            s1.setAutoCommit(false);   // 세션1: 트랜잭션 시작(수동 커밋)

            // 세션1이 잔액을 9999로 바꾼다 — 단, 아직 commit 안 함
            try (PreparedStatement ps = s1.prepareStatement("update accounts set balance = 9999 where id = 'A'")) {
                ps.executeUpdate();
            }
            System.out.println("세션1: A 잔액을 9999로 변경(아직 commit 안 함)");

            // 세션2가 지금 읽으면? -> 커밋 전이라 '옛 값(1000)'을 본다(READ COMMITTED, 격리성)
            System.out.println("세션2가 읽은 A 잔액 (commit 전) = " + balance(s2, "A") + "   <- 옛 값! 미커밋 변경은 안 보임");

            // 세션1이 commit
            s1.commit();
            System.out.println("세션1: commit 완료");

            // 이제 세션2가 다시 읽으면 새 값(9999)을 본다
            System.out.println("세션2가 읽은 A 잔액 (commit 후) = " + balance(s2, "A") + "   <- 이제 새 값 보임");
        }

        System.out.println("\n=> commit 전 변경은 다른 세션에 안 보이고(격리성), commit해야 보인다. H2 기본=READ COMMITTED.");
    }

    static int balance(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select balance from accounts where id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    static void setUp() throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.prepareStatement("create table if not exists accounts(id varchar(10) primary key, balance int)").executeUpdate();
            c.prepareStatement("delete from accounts").executeUpdate();
            c.prepareStatement("insert into accounts values ('A', 1000)").executeUpdate();
        }
    }
}
