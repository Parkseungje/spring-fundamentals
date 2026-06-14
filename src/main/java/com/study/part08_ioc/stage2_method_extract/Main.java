package com.study.part08_ioc.stage2_method_extract;

import com.study.part08_ioc.domain.User;

/**
 * [STAGE 2 데모] 메서드 추출로 중복이 사라졌다. 동작은 stage1과 같지만 구조가 깔끔해졌다.
 *
 * 핵심: add/get이 getConnection() 한 곳을 공유한다. DB 접속 정보가 바뀌어도 getConnection만 고치면 된다
 * (stage1은 모든 메서드를 고쳐야 했다). 단 getConnection이 아직 UserDao 안에 있어, "UserDao를 안 고치고
 * 연결 방식만 바꾸기"는 아직 불가 -> stage3.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        UserDao dao = new UserDao();
        dao.createTable();
        dao.add(new User("u2", "김유신"));
        System.out.println("[stage2] " + dao.get("u2"));
        System.out.println("=> 접속 코드가 getConnection() 한 곳으로 분리됨(중복 제거, 변경 한 곳). -> stage3: 추상화로 연결 교체.");
    }
}
