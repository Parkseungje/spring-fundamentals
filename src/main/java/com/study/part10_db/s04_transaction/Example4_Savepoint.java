package com.study.part10_db.s04_transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * [10.4 - 예시4] Savepoint — 트랜잭션을 '특정 지점까지만' 부분 롤백한다.
 *
 * 보통 rollback()은 트랜잭션 '전체'를 취소한다(Example1). 그런데 "앞부분 작업은 살리고, 뒷부분만 취소"
 * 하고 싶을 때가 있다. 그때 Savepoint(저장점)를 찍어 두고, 문제가 생기면 그 저장점까지만 rollback한다.
 *
 * 시나리오: 한 트랜잭션 안에서
 *   1) A에서 100 출금 (이건 꼭 살리고 싶음)
 *   2) 여기서 savepoint 찍음
 *   3) B에 100 입금 시도했는데 뭔가 잘못됨 -> savepoint까지만 롤백(=2 이후만 취소, 1은 유지)
 *   4) 최종 commit -> A 출금(1)만 반영되고, B 입금(3)은 취소됨
 *
 * 즉 전체 롤백(Example1)과 달리, savepoint 이후 작업만 선택적으로 되돌린다.
 */
public class Example4_Savepoint {

    static final String URL = "jdbc:h2:mem:part10_tx4;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        setUp();

        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false); // 트랜잭션 시작

            System.out.println("시작: A=" + balance(c, "A") + " B=" + balance(c, "B"));

            // 1) A에서 100 출금 (살리고 싶은 작업)
            update(c, "update accounts set balance = balance - 100 where id = 'A'");
            System.out.println("A에서 100 출금(이건 유지할 작업)");

            // 2) 저장점 찍기 — 여기 이후만 나중에 취소할 수 있다
            Savepoint sp = c.setSavepoint("afterWithdraw");

            // 3) B에 100 입금했는데 '문제가 생겼다'고 가정
            update(c, "update accounts set balance = balance + 100 where id = 'B'");
            System.out.println("B에 100 입금 시도... 그런데 문제 발생!");

            // savepoint까지만 롤백 -> 3만 취소, 1(A 출금)은 그대로 살아 있음
            c.rollback(sp); // 표준 JDBC: rollback(Savepoint) = 그 저장점 이후만 취소
            System.out.println("savepoint까지 부분 롤백 -> B 입금만 취소(A 출금은 유지)");

            // 4) 최종 commit
            c.commit();
            System.out.println("commit 완료");

            System.out.println("결과: A=" + balance(c, "A") + " B=" + balance(c, "B")
                    + "   <- A는 출금 반영(900), B는 입금 취소(1000)");
        }
        System.out.println("\n=> 전체 롤백(Example1)과 달리 savepoint 이후 작업만 선택 취소. 앞부분 작업은 유지된다.");
    }

    static void update(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.executeUpdate(); }
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
            c.prepareStatement("insert into accounts values ('B', 1000)").executeUpdate();
        }
    }
}
