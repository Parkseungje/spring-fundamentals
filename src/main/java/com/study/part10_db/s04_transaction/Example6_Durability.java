package com.study.part10_db.s04_transaction;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [10.4 - 예시6] D 지속성(Durability) — "commit된 데이터는 시스템이 꺼졌다 켜져도 살아남는다".
 *
 * 지속성은 "한 번 commit된 변경은 영구적이다. 정전·재시작이 나도 사라지지 않는다"는 성질이다. DB는 이를
 * 위해 commit 시점에 변경을 디스크(WAL 로그 등)에 안전하게 기록한다(fsync). 그래서 그 직후 죽어도 복구된다.
 *
 * 인메모리 DB(mem)는 프로세스가 끝나면 데이터가 사라져 지속성을 '보여줄' 수 없다. 그래서 이 예제만은
 * '파일 기반 H2'를 쓴다(디스크에 저장). '시스템 재시작'은 "연결을 완전히 닫아 DB를 내렸다가, 다시 새로
 * 연결해 여는 것"으로 시뮬레이션한다.
 *
 *  (A) 1차 실행: 데이터를 넣고 commit -> 연결/DB를 완전히 닫는다(= 종료/재시작).
 *  (B) 2차 실행: DB를 다시 연다 -> commit했던 데이터가 '그대로 남아 있다'(지속성).
 *  (C) 대조: commit하지 않은 변경은, DB를 닫았다 열면 사라진다(커밋해야 지속된다).
 *
 * 핵심: 격리성(Example2)은 "commit 전엔 남에게 안 보임", 지속성은 "commit 후엔 재시작해도 안 사라짐".
 * 둘 다 commit을 경계로 하지만 다른 성질이다.
 */
public class Example6_Durability {

    // 파일 기반 H2: 프로젝트 루트 기준 build/ 아래에 DB 파일이 생긴다(디스크에 저장 = 지속성 확인 가능).
    static final String DB_PATH = "./build/h2_durability_demo";
    static final String URL = "jdbc:h2:file:" + DB_PATH;

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        cleanFiles(); // 이전 실행 잔여 파일 제거(깨끗한 시작)

        // (A) 1차 "실행": 커밋된 데이터 + 커밋 안 한 데이터를 만들고 DB를 내린다.
        System.out.println("== (A) 1차: 데이터 작성 후 DB 종료(재시작 시뮬레이션) ==");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            c.setAutoCommit(false);
            c.prepareStatement("drop table if exists orders").executeUpdate();
            c.prepareStatement("create table orders(id int primary key, item varchar(20))").executeUpdate();
            c.prepareStatement("insert into orders values (1, 'committed-노트북')").executeUpdate();
            c.commit(); // ★ 이 행은 커밋 -> 지속되어야 함
            System.out.println("  insert(1, committed-노트북) + commit 완료");

            c.prepareStatement("insert into orders values (2, 'uncommitted-마우스')").executeUpdate();
            System.out.println("  insert(2, uncommitted-마우스) -> commit 안 함(여기서 '정전'이 난다고 가정)");
            // commit 없이 연결을 닫는다 = 갑작스런 종료. 커밋 안 한 2번은 사라져야 한다.
        } // try-with-resources 종료 -> 연결 닫힘 -> 파일 DB 내려감(= 시스템 종료)
        System.out.println("  DB 연결 종료(= 시스템 종료/재시작)\n");

        // (B)(C) 2차 "실행": DB를 새로 열어 무엇이 살아남았는지 확인
        System.out.println("== (B)(C) 2차: DB 재기동 후 데이터 확인 ==");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            System.out.println("  재기동 후 orders 내용:");
            boolean found1 = false, found2 = false;
            try (PreparedStatement ps = c.prepareStatement("select id, item from orders order by id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("    id=" + rs.getInt(1) + ", item=" + rs.getString(2));
                    if (rs.getInt(1) == 1) found1 = true;
                    if (rs.getInt(1) == 2) found2 = true;
                }
            }
            System.out.println("  (B) 커밋했던 1번 살아있나? " + found1 + "   <- true = 지속성!");
            System.out.println("  (C) 커밋 안 한 2번 살아있나? " + found2 + "   <- false = 커밋 안 하면 안 남는다");
        }

        cleanFiles();
        System.out.println("\n=> 지속성 = commit된 변경은 재시작해도 보존(1번 생존). 미커밋은 보존되지 않는다(2번 소멸).");
        System.out.println("   (mem DB로는 못 보여주는 성질이라 이 예제만 파일 기반 H2를 사용했다.)");
    }

    // H2 파일(.mv.db 등) 정리
    static void cleanFiles() {
        for (String suffix : new String[]{".mv.db", ".trace.db"}) {
            File f = new File(DB_PATH + suffix);
            if (f.exists()) {
                boolean ignored = f.delete();
            }
        }
    }
}
