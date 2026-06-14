package com.study.part11_jpa.s02_orm_mismatch;

import com.study.part11_jpa.domain.Member;
import com.study.part11_jpa.domain.Order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * [11.2] ORM 패러다임 — 객체와 관계형 DB의 '미스매치'를 직접 겪는다.
 *
 * 객체(OOP)와 관계형 DB는 다른 패러다임이다. 그 차이를 메우는 코드를 매번 손으로 쓰는 게 고통이고,
 * 그 고통을 프레임워크가 대신 메워주는 것이 ORM이다. 이 데모는 두 가지 미스매치를 raw JDBC로 보여준다.
 *
 *   (A) 연관(association) 미스매치: 객체는 'order.member'(참조), DB는 'member_id'(외래 키).
 *       그래서 DB에서 읽은 행을 객체 그래프로 만들려면 -> 회원을 따로 조회해 'new Member' 하고
 *       'order.setMember(member)'로 '손으로 연결'해야 한다(객체 그래프 수동 조립).
 *
 *   (B) 식별(identity) 미스매치: 객체는 '=='(인스턴스 동일성), DB는 'PK'(값 동일성).
 *       같은 회원(PK=1)을 두 번 조회하면, DB상 같은 행이지만 자바에선 'new'를 두 번 해 서로 다른 객체다(== false).
 *
 * ORM(Hibernate 등)은 (A) 객체 그래프 조립과 (B) 같은 PK는 같은 객체로 관리(영속성 컨텍스트)를 자동화해
 * 이 미스매치를 메운다. 단점도 있다: 학습곡선, N+1 문제, 튜닝 어려움(11.3~에서 본다).
 */
public class Main {

    static final String URL = "jdbc:h2:mem:part11_orm;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        setUp();

        // (A) 객체 그래프 '수동' 조립 — DB의 외래 키(member_id)를 객체 참조(order.member)로 손수 바꾼다.
        System.out.println("(A) 객체 그래프 수동 조립 (참조 vs 외래 키 미스매치):");
        Order order = loadOrderManually(1L);
        System.out.println("  조립된 객체 그래프 = " + order);
        System.out.println("  order.member.name = " + order.getMember().getName() + "  <- 이걸 위해 회원을 따로 new 해서 setMember 해야 했다");
        System.out.println();

        // (B) 식별 미스매치 — 같은 PK(1)를 두 번 조회하면 자바에선 서로 다른 객체(== false).
        System.out.println("(B) 식별 미스매치 (== vs PK):");
        Member m1 = loadMember(1L);
        Member m2 = loadMember(1L);
        System.out.println("  m1.id=" + m1.getId() + ", m2.id=" + m2.getId() + " (DB상 같은 행, 같은 PK)");
        System.out.println("  m1 == m2 ? " + (m1 == m2) + "   <- false! 'new'를 두 번 했으니 다른 인스턴스(객체 식별 != PK)");

        System.out.println();
        System.out.println("=> 객체(참조/==)와 DB(외래 키/PK)는 패러다임이 다르다. 이 간극을 메우는 코드를 매번 손으로");
        System.out.println("   쓰는 게 고통 -> ORM이 객체 그래프 조립·동일 PK 객체 관리를 자동화해 메운다(11.3 JPA).");
    }

    // 외래 키를 객체 참조로 '손으로' 바꿔 객체 그래프를 만든다(ORM이 없으면 이 작업을 매번 해야 한다).
    static Order loadOrderManually(Long orderId) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            Long memberId;
            String item;
            try (var ps = c.prepareStatement("select item, member_id from orders where id = ?")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    item = rs.getString("item");
                    memberId = rs.getLong("member_id");   // DB는 '숫자(외래 키)'만 준다
                }
            }
            Member member = loadMember(memberId);          // 회원을 '따로' 조회해서
            return new Order(orderId, item, member);       // 객체로 '손수 연결'(setMember 역할)
        }
    }

    static Member loadMember(Long memberId) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", "");
             var ps = c.prepareStatement("select id, name from member where id = ?")) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Member(rs.getLong("id"), rs.getString("name"));   // 매번 new -> (B) 식별 미스매치
            }
        }
    }

    static void setUp() throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement st = c.createStatement()) {
            st.execute("create table member(id bigint primary key, name varchar(20))");
            st.execute("create table orders(id bigint primary key, item varchar(20), member_id bigint)");
            st.execute("insert into member values (1,'홍길동')");
            st.execute("insert into orders values (1,'노트북',1)");
        }
    }
}
