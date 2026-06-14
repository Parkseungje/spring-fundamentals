package com.study.part08_ioc.stage1_traditional;

import com.study.part08_ioc.domain.User;

/**
 * [STAGE 1 데모] 전통 DAO는 '동작은 한다'. 문제는 동작이 아니라 '구조'다.
 *
 * 실행하면 add/get이 정상 동작한다. 하지만 UserDao.java를 열어보면 add와 get에 드라이버 로딩+접속
 * 코드가 '복붙'돼 있다. DB를 MySQL로 바꾸면? 두 메서드(나아가 모든 메서드)를 다 고쳐야 한다.
 * 이 '변경 1번 = 수정 N곳'이 stage2부터 해결할 대상이다.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        UserDao dao = new UserDao();
        dao.createTable();

        dao.add(new User("u1", "홍길동"));
        User found = dao.get("u1");

        System.out.println("[stage1] 저장 후 조회 결과 = " + found);
        System.out.println("=> 동작은 한다. 그러나 add/get에 '드라이버 로딩+접속' 코드가 중복되어 있다.");
        System.out.println("   DB가 바뀌면 모든 메서드를 고쳐야 한다(SRP/OCP/DIP 위반). -> stage2에서 분리.");
    }
}
