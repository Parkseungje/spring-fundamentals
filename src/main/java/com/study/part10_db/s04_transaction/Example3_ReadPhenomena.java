package com.study.part10_db.s04_transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.4 - 예시3] 격리 수준이 막아주는 '읽기 이상현상' 3종 재현 — Non-Repeatable Read / Phantom Read.
 *
 * Example2는 Dirty Read(미커밋 값 읽기)를 다뤘다(정확히는 READ COMMITTED가 그것을 막는 것을 보였다).
 * 여기서는 나머지 두 이상현상을, '한 트랜잭션 안에서 같은 읽기를 두 번' 하는 사이 다른 트랜잭션이 끼어드는
 * 시나리오로 재현한다.
 *
 *  (A) Non-Repeatable Read(반복 불가능 읽기): 같은 '행'을 두 번 읽었는데, 그 사이 다른 트랜잭션이 그 행을
 *      수정·커밋해서 두 번째 값이 달라진다. -> READ COMMITTED에서는 발생. REPEATABLE READ부터 방지.
 *  (B) 같은 시나리오를 격리 수준만 REPEATABLE READ로 올리면, 첫 읽기의 스냅샷이 유지되어 두 번째도 같은 값
 *      (반복 가능). 이것으로 "격리 수준을 올리면 이 현상이 막힌다"를 대조한다.
 *  (C) Phantom Read(팬텀 리드): 같은 '조건'으로 두 번 조회했는데, 그 사이 다른 트랜잭션이 행을 삽입·커밋해서
 *      두 번째 결과의 '행 개수'가 달라진다(없던 행이 유령처럼 나타남). -> READ COMMITTED에서 발생.
 *
 * H2 기본 격리 수준은 READ COMMITTED라 (A)(C)가 재현된다. (B)는 명시적으로 격리 수준을 올려 비교한다.
 */
public class Example3_ReadPhenomena {

    static final String URL = "jdbc:h2:mem:part10_tx3;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");

        nonRepeatableRead(Connection.TRANSACTION_READ_COMMITTED, "(A) READ COMMITTED");
        nonRepeatableRead(Connection.TRANSACTION_REPEATABLE_READ, "(B) REPEATABLE READ");
        phantomRead();
    }

    // (A)/(B) 같은 행을 두 번 읽는 사이 다른 트랜잭션이 수정·커밋 -> 격리 수준에 따라 결과가 갈린다.
    static void nonRepeatableRead(int isolation, String label) throws Exception {
        setUp();
        System.out.println("== " + label + " — Non-Repeatable Read 시도 ==");
        try (Connection reader = DriverManager.getConnection(URL, "sa", "");
             Connection writer = DriverManager.getConnection(URL, "sa", "")) {

            reader.setAutoCommit(false);
            reader.setTransactionIsolation(isolation); // 격리 수준 지정
            int first = balance(reader, "A");
            System.out.println("  reader 1차 읽기: A = " + first);

            // 그 사이 writer가 A를 수정하고 커밋
            writer.setAutoCommit(true);
            try (PreparedStatement ps = writer.prepareStatement("update accounts set balance = 2000 where id = 'A'")) {
                ps.executeUpdate();
            }
            System.out.println("  writer: A를 2000으로 수정 + 커밋");

            int second = balance(reader, "A"); // 같은 트랜잭션에서 다시 읽기
            System.out.println("  reader 2차 읽기: A = " + second);
            reader.commit();

            if (first != second) {
                System.out.println("  => 두 읽기가 다르다(" + first + " -> " + second + ") = Non-Repeatable Read 발생!\n");
            } else {
                System.out.println("  => 두 읽기가 같다(" + first + ") = 반복 가능(격리 수준이 막아줌)\n");
            }
        }
    }

    // (C) 같은 '조건'으로 두 번 count 하는 사이 행이 삽입·커밋 -> 행 개수가 늘어난다(팬텀).
    static void phantomRead() throws Exception {
        setUp();
        System.out.println("== (C) READ COMMITTED — Phantom Read 시도 ==");
        try (Connection reader = DriverManager.getConnection(URL, "sa", "");
             Connection writer = DriverManager.getConnection(URL, "sa", "")) {

            reader.setAutoCommit(false);
            reader.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            int firstCount = countRich(reader);
            System.out.println("  reader 1차 조회: balance >= 1000 인 행 수 = " + firstCount);

            // 그 사이 writer가 조건에 맞는 '새 행'을 삽입하고 커밋
            writer.setAutoCommit(true);
            try (PreparedStatement ps = writer.prepareStatement("insert into accounts values ('C', 5000)")) {
                ps.executeUpdate();
            }
            System.out.println("  writer: 새 행 C(5000) 삽입 + 커밋");

            int secondCount = countRich(reader);
            System.out.println("  reader 2차 조회: balance >= 1000 인 행 수 = " + secondCount);
            reader.commit();

            System.out.println("  => 행 수가 " + firstCount + " -> " + secondCount + "로 늘었다 = Phantom Read(유령 행)!");
        }
    }

    static int balance(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select balance from accounts where id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    static int countRich(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select count(*) from accounts where balance >= 1000");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
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
