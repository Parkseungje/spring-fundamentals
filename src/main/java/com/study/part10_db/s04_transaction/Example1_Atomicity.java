package com.study.part10_db.s04_transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.4 - 예시1] 원자성(Atomicity) — "모두 성공 or 모두 실패".
 *
 * 트랜잭션 = 한 단위로 취급되는 작업 묶음. 대표 예가 '계좌 이체'다(A 출금 + B 입금은 둘 다 되거나 둘 다
 * 안 되어야 한다). 중간에 실패했는데 출금만 반영되면 돈이 사라진다.
 *
 * JDBC에서 트랜잭션 다루기:
 *   - conn.setAutoCommit(false): "이제부터 내가 commit 할 때까지 한 묶음" (자동 커밋 끔)
 *   - conn.commit():   묶음 전체를 영구 반영
 *   - conn.rollback(): 묶음 전체를 취소(중간까지 한 것도 없던 일로)
 *
 * 이 데모는 같은 이체를 (A) 중간 실패 -> rollback / (B) 정상 -> commit 두 경우로 보여준다.
 * (A)에서 출금 후 예외가 나도 rollback 덕분에 잔액이 '둘 다 원래대로' 돌아온다(원자성).
 */
public class Example1_Atomicity {

    static final String URL = "jdbc:h2:mem:part10_tx1;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        setUp();

        System.out.println("초기 잔액: A=" + balance("A") + ", B=" + balance("B"));

        // (A) 이체 도중 실패 -> rollback -> 둘 다 원래대로
        System.out.println("\n(A) 이체 300 (도중 예외 발생 -> rollback):");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false);                       // 트랜잭션 시작
            try {
                update(c, "A", -300);                     // 출금(아직 미확정)
                if (true) throw new RuntimeException("이체 중 장애 발생!");  // 일부러 중간 실패
                update(c, "B", +300);                     // (도달 못 함)
                c.commit();
            } catch (RuntimeException e) {
                c.rollback();                             // ★ 전체 취소 — 출금도 없던 일로
                System.out.println("  예외: " + e.getMessage() + " -> rollback");
            }
        }
        System.out.println("  결과 잔액: A=" + balance("A") + ", B=" + balance("B") + "  (둘 다 원래대로 = 원자성)");

        // (B) 정상 이체 -> commit -> 둘 다 반영
        System.out.println("\n(B) 이체 300 (정상 -> commit):");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false);
            update(c, "A", -300);
            update(c, "B", +300);
            c.commit();                                   // 묶음 전체 영구 반영
        }
        System.out.println("  결과 잔액: A=" + balance("A") + ", B=" + balance("B") + "  (둘 다 반영)");

        System.out.println("\n=> 트랜잭션은 '모두 성공(commit) 또는 모두 실패(rollback)'를 보장한다 = 원자성(Atomicity).");
    }

    static void update(Connection c, String id, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("update accounts set balance = balance + ? where id = ?")) {
            ps.setInt(1, delta);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    static int balance(String id) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "");
             PreparedStatement ps = c.prepareStatement("select balance from accounts where id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    static void setUp() throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.prepareStatement("create table if not exists accounts(id varchar(10) primary key, balance int)").executeUpdate();
            c.prepareStatement("delete from accounts").executeUpdate();
            c.prepareStatement("insert into accounts values ('A', 1000), ('B', 1000)").executeUpdate();
        }
    }
}
