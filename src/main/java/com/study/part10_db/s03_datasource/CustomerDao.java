package com.study.part10_db.s03_datasource;

import com.study.part10_db.domain.Customer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.3] DataSource 추상화 — DAO는 '연결을 어떻게 얻는지' 모른다(DIP).
 *
 * 10.2에서 풀(HikariCP)을 봤다. 그런데 풀 구현마다(DriverManager vs HikariCP vs DBCP2) 연결을 얻는
 * '사용법'이 다르면, 구현을 바꿀 때 DAO 코드까지 다 바뀐다. 그 문제를 푸는 표준이 javax.sql.DataSource다.
 *
 * DataSource = '연결 공급자'의 표준 인터페이스. 핵심 메서드는 getConnection() 하나뿐이다.
 * 이 DAO는 구체 구현(HikariDataSource 등)이 아니라 'DataSource 인터페이스'에만 의존한다(생성자 주입).
 * 그래서 어떤 구현을 주입하든 DAO 코드는 한 줄도 안 바뀐다. → PART 8.4 ConnectionMaker와 똑같은 사상(DIP).
 */
public class CustomerDao {

    private final DataSource dataSource;   // 구체 구현이 아니라 '인터페이스'에 의존

    public CustomerDao(DataSource dataSource) {   // 어떤 DataSource 구현이든 외부에서 주입
        this.dataSource = dataSource;
    }

    public void add(Customer c) throws SQLException {
        try (Connection conn = dataSource.getConnection();   // ★ 어떤 구현이든 getConnection() 하나로 통일
             PreparedStatement ps = conn.prepareStatement("insert into customers(id, name, age) values (?, ?, ?)")) {
            ps.setString(1, c.getId());
            ps.setString(2, c.getName());
            ps.setInt(3, c.getAge());
            ps.executeUpdate();
        }
    }

    public Customer get(String id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("select id, name, age from customers where id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Customer(rs.getString("id"), rs.getString("name"), rs.getInt("age"));
            }
        }
    }

    public void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.prepareStatement("create table if not exists customers(" +
                    "id varchar(50) primary key, name varchar(50), age int)").executeUpdate();
        }
    }
}
