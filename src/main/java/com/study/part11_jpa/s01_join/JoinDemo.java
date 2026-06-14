package com.study.part11_jpa.s01_join;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * [11.1] SQL JOIN — 관계형 DB의 본질.
 *
 * 왜 JOIN인가: 관계형 DB는 '정규화'로 중복을 제거한다. 그 결과 정보가 여러 테이블에 흩어지므로, 합쳐
 * 보려면 JOIN으로 연결해야 한다. (객체로 치면 employee.department 참조를, DB에선 외래 키 dept_id로 표현)
 *
 * JOIN 종류:
 *   - INNER JOIN : 양쪽에 다 매칭되는 행만(교집합). 부서가 없는 직원은 빠진다.
 *   - LEFT  JOIN : 왼쪽(직원) 전부 + 매칭되는 부서(없으면 NULL). 실무에서 가장 자주 쓴다.
 *   - RIGHT JOIN : 오른쪽 전부 + 매칭. 거의 안 씀(LEFT로 뒤집어 표현).
 *   - FULL OUTER: 양쪽 전부(합집합). MySQL은 미지원(LEFT UNION RIGHT로 대체).
 *
 * 활용 예: "부서가 없는 직원 찾기" = LEFT JOIN 후 부서 쪽이 NULL인 행만(WHERE d.id IS NULL).
 *   (ON 조건과 WHERE 조건은 OUTER JOIN에서 의미가 다르다 — ON은 '매칭 규칙', WHERE는 '결과 필터'.)
 */
public class JoinDemo {

    static final String URL = "jdbc:h2:mem:part11_join;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement st = c.createStatement()) {
            // 정규화된 두 테이블: 부서(department)와 직원(employee). 직원은 dept_id로 부서를 참조(외래 키).
            st.execute("create table department(id int primary key, name varchar(20))");
            st.execute("create table employee(id int primary key, name varchar(20), dept_id int)");
            st.execute("insert into department values (1,'개발'),(2,'영업')");
            // 박무소속은 dept_id가 NULL(부서 없음)
            st.execute("insert into employee values (1,'김개발',1),(2,'이영업',2),(3,'박무소속',null)");

            System.out.println("== INNER JOIN (양쪽 매칭만 = 박무소속 제외) ==");
            print(st, "select e.name emp, d.name dept from employee e " +
                    "inner join department d on e.dept_id = d.id order by e.id");

            System.out.println("== LEFT JOIN (직원 전부 + 부서, 없으면 NULL) ==");
            print(st, "select e.name emp, d.name dept from employee e " +
                    "left join department d on e.dept_id = d.id order by e.id");

            System.out.println("== 부서 없는 직원 (LEFT JOIN + WHERE d.id IS NULL) ==");
            print(st, "select e.name emp from employee e " +
                    "left join department d on e.dept_id = d.id where d.id is null");
        }

        System.out.println("=> 정규화로 흩어진 정보를 JOIN으로 합친다. INNER=교집합(매칭만), LEFT=왼쪽 전부(없으면 NULL).");
        System.out.println("   'LEFT JOIN + WHERE NULL'로 '매칭 안 되는 행(부서 없는 직원)'을 찾는 게 대표 활용.");
    }

    static void print(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder sb = new StringBuilder("  ");
                for (int i = 1; i <= cols; i++) {
                    String v = rs.getString(i);
                    sb.append(rs.getMetaData().getColumnLabel(i)).append("=").append(v == null ? "NULL" : v).append("  ");
                }
                System.out.println(sb);
            }
        }
        System.out.println();
    }
}
