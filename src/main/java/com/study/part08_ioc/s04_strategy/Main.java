package com.study.part08_ioc.s04_strategy;

import com.study.part08_ioc.domain.User;

/**
 * [8.4 데모] UserDao는 그대로 두고, 주입하는 ConnectionMaker만 바꿔 끼운다.
 *
 * 핵심 관찰: 같은 UserDao 클래스에 NConnectionMaker를 주든 DConnectionMaker를 주든, UserDao 코드는
 * 한 줄도 안 바뀐다(OCP). "어떤 구현을 쓸지"의 결정이 UserDao 밖(여기 Main)으로 나왔다.
 * 단, 아직 'new NConnectionMaker()'와 조립(주입)을 Main이 직접 한다 -> 8.5/8.6에서 컨테이너에 위임.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 클라이언트(Main)가 '어떤 전략을 쓸지' 정해서 주입한다.
        ConnectionMaker cm = new NConnectionMaker();   // D로 바꾸려면 이 한 줄만: new DConnectionMaker()
        UserDao dao = new UserDao(cm);                 // 생성자 주입(합성)

        dao.createTable();
        dao.add(new User("s4", "전략패턴"));
        System.out.println("[8.4] " + dao.get("s4"));
        System.out.println("=> UserDao는 ConnectionMaker(인터페이스)에만 의존. 구현 교체에도 UserDao 수정 0(OCP/DIP).");
        System.out.println("   하지만 'new NConnectionMaker()'와 주입을 아직 Main이 직접 함 -> 8.5/8.6: 컨테이너(IoC).");
    }
}
