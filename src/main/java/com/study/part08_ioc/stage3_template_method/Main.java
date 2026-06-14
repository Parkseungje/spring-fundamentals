package com.study.part08_ioc.stage3_template_method;

import com.study.part08_ioc.domain.User;

/**
 * [STAGE 3 데모] 같은 add/get 흐름(부모)을 두 고객사(자식)가 각자 연결 방법만 바꿔 쓴다.
 *
 * 핵심 관찰: UserDao(부모)의 코드는 한 줄도 안 건드리고, 연결 방법이 다른 NUserDao/DUserDao를 갈아끼웠다.
 * 하지만 "어떤 걸 쓸지"가 'new NUserDao()'처럼 코드에 박혀 있다(컴파일 타임 결정) — stage4에서 이걸 푼다.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        UserDao nDao = new NUserDao();   // N 고객사용 (어떤 자식인지 여기서 결정 = 컴파일 타임 결합)
        nDao.createTable();
        nDao.add(new User("n1", "N-홍길동"));
        System.out.println("[stage3 N] " + nDao.get("n1"));

        UserDao dDao = new DUserDao();   // D 고객사용
        dDao.createTable();
        dDao.add(new User("d1", "D-임꺽정"));
        System.out.println("[stage3 D] " + dDao.get("d1"));

        System.out.println("=> 부모(UserDao) 흐름은 그대로, 자식이 연결만 교체(템플릿 메소드). 단 'new NUserDao()'처럼");
        System.out.println("   어떤 구현을 쓸지가 코드에 박힘(상속의 한계). -> stage4: 인터페이스+합성으로 분리.");
    }
}
