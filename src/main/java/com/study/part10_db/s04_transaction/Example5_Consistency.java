package com.study.part10_db.s04_transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.4 - 예시5] C 일관성(Consistency) — 트랜잭션은 'DB를 늘 올바른 상태로만' 옮긴다.
 *
 * 일관성이란 "트랜잭션이 끝났을 때 DB가 정의된 모든 규칙(제약조건·불변식)을 여전히 만족한다"는 것이다.
 * 규칙을 깨뜨리는 변경은 DB가 거부하고, 그러면 (원자성과 함께) 트랜잭션이 통째로 롤백되어 DB는 변경 전의
 * '올바른 상태' 그대로 남는다. 즉 "잘못된 상태로는 절대 안 넘어간다".
 *
 * 이 예제는 두 종류의 규칙으로 일관성을 보여준다:
 *  (A) 제약조건(CHECK balance >= 0): 잔액을 음수로 만들려는 트랜잭션은 DB가 거부 -> 롤백 -> 잔액 그대로.
 *      "DB 스스로 지키는 규칙"을 깨는 변경은 커밋될 수 없다.
 *  (B) 비즈니스 불변식(두 계좌 합계 = 2000 유지): 이체는 한쪽에서 빼고 다른 쪽에 같은 만큼 더해야 '합'이
 *      보존된다. 출금만 하고 입금을 빠뜨리면 합이 깨진다 -> 그런 트랜잭션은 롤백해야 일관성이 유지된다.
 *      (원자성이 일관성의 '도구'임을 보여줌 — 모두-or-아무것도가 불변식을 지킨다.)
 *
 * 핵심 구분: 원자성=실행 방식(모두 or 아무것도), 일관성=결과 상태가 규칙을 만족(올바름). 원자성·격리성·
 * 제약조건이 함께 작동해 일관성을 '결과적으로' 보장한다.
 */
public class Example5_Consistency {

    static final String URL = "jdbc:h2:mem:part10_tx5;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        setUp();

        // (A) 제약조건 위반은 커밋될 수 없다 -> DB가 올바른 상태로 남는다
        System.out.println("== (A) 제약조건(CHECK balance >= 0) 위반 -> 거부 -> 롤백 ==");
        System.out.println("  시작: A = " + balanceWith(URL, "A"));
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false);
            // A(1000)에서 1500을 빼려 한다 -> 잔액 -500 -> CHECK 제약 위반
            try (PreparedStatement ps = c.prepareStatement("update accounts set balance = balance - 1500 where id = 'A'")) {
                ps.executeUpdate();
                c.commit(); // 여기서 제약 위반이 터진다(H2는 commit/실행 시 CHECK 검사)
                System.out.println("  (도달하면 안 됨) 음수 잔액이 커밋됨");
            } catch (SQLException e) {
                c.rollback();
                System.out.println("  제약 위반으로 거부됨 -> 롤백: " + firstLine(e.getMessage()));
            }
        }
        System.out.println("  결과: A = " + balanceWith(URL, "A") + "   <- 음수로 안 바뀜(일관성 유지)\n");

        // (B) 비즈니스 불변식(합계=2000): 출금만 하고 입금을 빠뜨리면 합이 깨진다 -> 롤백해야 일관성 유지
        System.out.println("== (B) 비즈니스 불변식: A+B 합계는 항상 2000 유지 ==");
        System.out.println("  시작 합계 = " + total() + " (A+B)");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false);
            update(c, "update accounts set balance = balance - 300 where id = 'A'"); // 출금
            System.out.println("  A에서 300 출금... 그런데 입금 단계에서 오류 발생!(합계가 1700로 깨진 상태)");
            boolean depositFailed = true; // 입금 중 장애 가정
            if (depositFailed) {
                c.rollback(); // 불변식이 깨진 채 커밋하지 않고 통째 롤백 -> 합계 2000 복구
                System.out.println("  -> 롤백: 불변식이 깨진 중간 상태를 커밋하지 않는다(원자성이 일관성을 지킴)");
            } else {
                c.commit();
            }
        }
        System.out.println("  결과 합계 = " + total() + " (A+B)   <- 2000 유지(일관성)\n");

        System.out.println("=> 일관성 = '규칙(제약·불변식)을 만족하는 올바른 상태로만 이동'. 규칙을 깨는 변경은");
        System.out.println("   DB가 거부하거나(제약) 롤백되어(원자성) 결코 커밋되지 않는다.");
    }

    static int total() throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "");
             PreparedStatement ps = c.prepareStatement("select sum(balance) from accounts");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    static int balanceWith(String url, String id) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             PreparedStatement ps = c.prepareStatement("select balance from accounts where id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    static void update(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.executeUpdate(); }
    }

    static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    static void setUp() throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.prepareStatement("drop table if exists accounts").executeUpdate();
            // CHECK 제약: 잔액은 음수가 될 수 없다(DB 스스로 지키는 규칙)
            c.prepareStatement("create table accounts(id varchar(10) primary key, balance int check (balance >= 0))").executeUpdate();
            c.prepareStatement("insert into accounts values ('A', 1000)").executeUpdate();
            c.prepareStatement("insert into accounts values ('B', 1000)").executeUpdate();
        }
    }
}
