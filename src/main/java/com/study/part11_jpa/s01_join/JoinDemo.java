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

            // ── (추가1) CROSS JOIN = 카테시안 곱: JOIN의 본질 ──
            // JOIN은 사실 "두 테이블의 '모든 행 조합'을 만든 뒤 ON 조건으로 거르는 것"이다. CROSS JOIN은
            // 그 '거르기 전' 상태 = 모든 조합. 직원3 x 부서2 = 6행이 나온다. 그래서 ON을 빠뜨리면(또는
            // 잘못 쓰면) 결과가 N x M으로 폭발한다(흔한 사고). INNER JOIN = CROSS JOIN + ON 필터인 셈.
            System.out.println("== CROSS JOIN (카테시안 곱: 모든 조합 = 직원3 x 부서2 = 6행) ==");
            print(st, "select e.name emp, d.name dept from employee e cross join department d order by e.id, d.id");

            // ── (추가2) 1:N JOIN의 '행 뻥튀기' + DISTINCT ──
            // 부서(1)에 직원(N)이 매달린 1:N 관계를 '부서 기준'으로 조인하면, 직원 수만큼 부서 행이 중복된다.
            // 개발 부서에 직원 2명을 넣어 두고 부서를 직원과 조인하면 '개발'이 2번 나온다 -> 부서 목록만
            // 원하면 중복이 생긴다. DISTINCT로 제거할 수 있다. (이것이 PART 14 JPA 컬렉션 페치 조인에서
            // 엔티티가 중복되는 문제의 뿌리 — 거기선 distinct로 푼다.)
            st.execute("insert into employee values (4,'최개발',1)"); // 개발 부서에 2번째 직원 추가
            System.out.println("== 1:N JOIN 행 뻥튀기 (부서를 직원과 조인 -> 개발이 직원 수만큼 중복) ==");
            print(st, "select d.name dept, e.name emp from department d " +
                    "join employee e on e.dept_id = d.id order by d.id, e.id");
            System.out.println("== DISTINCT로 '부서 목록' 중복 제거 ==");
            print(st, "select distinct d.name dept from department d " +
                    "join employee e on e.dept_id = d.id order by d.name");

            // ── (추가3) N:M(다대다)과 중간(조인) 테이블 ──
            // 학생과 과목은 다대다(한 학생이 여러 과목, 한 과목에 여러 학생)다. 관계형 DB는 다대다를 직접
            // 표현 못 해, '중간 테이블(enrollment)'에 (학생, 과목) 쌍을 저장해 1:N + N:1 두 개로 푼다.
            // 조회는 세 테이블(학생-중간-과목)을 이어서 JOIN한다. (PART 14 JPA @ManyToMany/중간 엔티티의 토대.)
            st.execute("create table student(id int primary key, name varchar(20))");
            st.execute("create table course(id int primary key, title varchar(20))");
            st.execute("create table enrollment(student_id int, course_id int)"); // 중간 테이블
            st.execute("insert into student values (1,'학생A'),(2,'학생B')");
            st.execute("insert into course values (10,'자바'),(20,'DB')");
            // 학생A: 자바+DB, 학생B: 자바 -> 다대다 관계를 쌍으로 저장
            st.execute("insert into enrollment values (1,10),(1,20),(2,10)");
            System.out.println("== N:M JOIN (학생-중간(enrollment)-과목 세 테이블 연결) ==");
            print(st, "select s.name student, c.title course from student s " +
                    "join enrollment en on en.student_id = s.id " +
                    "join course c on c.id = en.course_id order by s.id, c.id");

            // ── (추가4) UNION vs UNION ALL ──
            // JOIN은 두 테이블을 '가로로'(열을 붙여) 합쳤다. UNION은 두 '조회 결과'를 '세로로'(행을 이어) 합친다.
            // 조건: 두 SELECT의 '컬럼 수와 타입'이 같아야 한다.
            //
            // 핵심: UNION은 "사람을 거르는 것"이 아니라 "독립적인 두 조회의 '결과 줄'을 쌓는 것"이다.
            //   조회A: where dept_id=1        -> 김개발, 최개발  (2줄)
            //   조회B: where name like '%개발%' -> 김개발, 최개발  (2줄)
            //   같은 김개발이라도 '조회A가 만든 1줄'과 '조회B가 만든 1줄'은 별개의 줄이다.
            //
            // UNION ALL : 두 결과를 그대로 이어 붙인다(중복 검사 안 함) -> 2 + 2 = 4줄(김개발 2, 최개발 2). 더 빠름.
            // UNION     : 그렇게 쌓은 뒤 '똑같은 줄'을 합친다(중복 제거) -> 4줄 -> 2줄.
            // 즉 두 조회 결과가 겹칠 때만 개수가 갈린다(안 겹치면 UNION과 UNION ALL 결과 수가 같다).
            //
            // '똑같은 줄'의 기준: 선택한 '모든 컬럼의 값'이 행 단위로 전부 같을 때(여러 컬럼이면 전부 일치해야 함).
            //   컬럼 이름은 무관(위치별 값으로 비교, 결과명은 첫 SELECT 것). 컬럼 수/타입 일치는 전제 조건.
            //   NULL은 일반 비교와 달리 UNION 중복 판정에선 NULL끼리 '같다'고 보고 합친다.
            System.out.println("== UNION (두 조회 결과를 세로로 합치되 중복 제거) ==");
            print(st, "select name from employee where dept_id = 1 " +
                    "union " +
                    "select name from employee where name like '%개발%' order by name");
            System.out.println("== UNION ALL (중복도 그대로) ==");
            print(st, "select name from employee where dept_id = 1 " +
                    "union all " +
                    "select name from employee where name like '%개발%' order by name");

            // ── (추가5) FULL OUTER JOIN 흉내 = LEFT UNION RIGHT방향 ──
            // 양쪽의 '매칭 안 되는 행'을 모두 보고 싶을 때가 FULL OUTER다. MySQL은 FULL OUTER를 지원하지 않아,
            // 'employee 기준 LEFT' UNION 'department 기준 LEFT'로 흉내 낸다(UNION이 겹치는 매칭 행은 합쳐 줌).
            // 직원 없는 부서 '인사'를 추가해 두 종류의 NULL(부서 없는 직원 / 직원 없는 부서)이 모두 나오게 한다.
            st.execute("insert into department values (3,'인사')"); // 직원이 없는 부서
            System.out.println("== FULL OUTER 흉내 (LEFT UNION 반대방향 LEFT) ==");
            print(st, "select e.name emp, d.name dept from employee e left join department d on e.dept_id = d.id " +
                    "union " +
                    "select e.name emp, d.name dept from department d left join employee e on e.dept_id = d.id " +
                    "order by emp nulls last");

            // ── (추가6) GROUP BY + 집계 함수 ──
            // GROUP BY는 '같은 값끼리 행을 묶어' 그룹마다 집계(count/sum/avg/max/min)한다. 여기선 dept_id로
            // 묶어 '부서별 직원 수'를 센다. (현재 employee: 개발 2명, 영업 1명, 무소속(null) 1명)
            System.out.println("== GROUP BY (부서id별 직원 수) ==");
            print(st, "select dept_id, count(*) cnt from employee group by dept_id order by dept_id nulls last");

            // ── (추가7) JOIN + GROUP BY ──
            // 부서 '이름'별 직원 수를 보려면 department와 JOIN한 뒤 부서로 묶는다. LEFT JOIN을 쓰고 count(e.id)로
            // 세면 직원 0명인 부서(인사)도 0으로 나온다. (주의: count(*)는 LEFT의 NULL 행도 1로 세어 인사가 1이 됨
            //  -> '직원 수'를 정확히 세려면 count(e.id)처럼 '직원 컬럼'을 센다.)
            System.out.println("== JOIN + GROUP BY (부서 이름별 직원 수, 직원 0명 부서 포함) ==");
            print(st, "select d.name dept, count(e.id) cnt from department d " +
                    "left join employee e on e.dept_id = d.id group by d.name order by cnt desc");

            // ── (추가8) HAVING (그룹 집계 결과 필터) ──
            // WHERE는 '묶기 전 개별 행'을 거르고, HAVING은 '묶은 뒤 그룹'을 거른다. "직원 2명 이상인 부서만".
            System.out.println("== HAVING (직원 2명 이상인 부서만) ==");
            print(st, "select d.name dept, count(e.id) cnt from department d " +
                    "left join employee e on e.dept_id = d.id group by d.name having count(e.id) >= 2");
        }

        System.out.println("=> 정규화로 흩어진 정보를 JOIN으로 합친다. INNER=교집합(매칭만), LEFT=왼쪽 전부(없으면 NULL).");
        System.out.println("   'LEFT JOIN + WHERE NULL'로 '매칭 안 되는 행(부서 없는 직원)'을 찾는 게 대표 활용.");
        System.out.println("   JOIN의 본질은 카테시안 곱+ON 필터. 1:N 조인은 행이 뻥튀기되어 DISTINCT가 필요할 수 있고,");
        System.out.println("   N:M은 중간 테이블로 풀어 세 테이블을 JOIN한다(JPA 연관관계의 토대).");
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
