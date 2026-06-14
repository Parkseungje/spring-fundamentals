package com.study.part08_ioc.s02_separation;

import com.study.part08_ioc.domain.User;

/**
 * [8.2 데모] 관심사 분리의 두 단계를 차례로 확인한다.
 *
 * 1단계(메서드 추출): 접속 코드가 getConnection() 한 곳으로 모여 중복이 사라진다(수정 N곳 -> 1곳).
 * 2단계(추상클래스/템플릿 메소드): 부모(AbstractUserDao)의 흐름은 그대로, 자식(N/DUserDao)이 연결 방법만
 *   갈아끼운다. 단 "어떤 자식을 쓸지"가 'new NUserDao()'처럼 코드에 박힌다(상속의 한계 -> 8.3/8.4).
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("== 8.2 - 1단계: 메서드 추출 ==");
        UserDaoMethodExtract v1 = new UserDaoMethodExtract();
        v1.createTable();
        v1.add(new User("u2", "김유신"));
        System.out.println("[1단계] " + v1.get("u2"));
        System.out.println("  -> 접속 코드가 getConnection() 한 곳으로 분리됨(중복 제거, 변경 한 곳).");
        System.out.println();

        System.out.println("== 8.2 - 2단계: 추상클래스(템플릿 메소드) ==");
        AbstractUserDao nDao = new NUserDao();   // 어떤 자식인지 여기서 결정 = 컴파일 타임 결합
        nDao.createTable();
        nDao.add(new User("n1", "N-홍길동"));
        System.out.println("[2단계 N] " + nDao.get("n1"));

        AbstractUserDao dDao = new DUserDao();
        dDao.createTable();
        dDao.add(new User("d1", "D-임꺽정"));
        System.out.println("[2단계 D] " + dDao.get("d1"));

        System.out.println("  -> 부모 흐름은 그대로, 자식이 연결만 교체(템플릿 메소드). 단 'new NUserDao()'처럼");
        System.out.println("     어떤 구현을 쓸지가 코드에 박힘(상속의 한계). -> 8.3 분석 -> 8.4 인터페이스+합성.");
    }
}
