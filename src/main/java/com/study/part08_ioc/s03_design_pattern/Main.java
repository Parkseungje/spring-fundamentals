package com.study.part08_ioc.s03_design_pattern;

import com.study.part08_ioc.domain.User;
import com.study.part08_ioc.s02_separation.AbstractUserDao;
import com.study.part08_ioc.s02_separation.DUserDao;
import com.study.part08_ioc.s02_separation.NUserDao;

/**
 * [8.3 데모] 8.2의 추상클래스 구조를 '디자인 패턴' 관점으로 분석하고 한계를 짚는다.
 *
 * 8.3은 새 구조를 만드는 단원이 아니라, 8.2에서 만든 구조(AbstractUserDao + N/DUserDao)가 사실은
 * 두 가지 고전 디자인 패턴의 동시 적용임을 '인식'하고, 그 상속 기반 방식의 한계를 정리하는 단원이다.
 * 그래서 이 Main은 8.2의 클래스를 그대로 가져와 실행하며 패턴과 한계를 해설한다.
 *
 * 적용된 두 패턴:
 *   - 템플릿 메소드 패턴: 슈퍼클래스(AbstractUserDao)가 흐름(add/get)을 고정하고, 변하는 한 부분
 *     (getConnection)만 서브클래스가 구현. (Spring 'JdbcTemplate'의 Template이 이 이름에서 유래.)
 *   - 팩토리 메소드 패턴: 객체 생성(Connection 만들기)을 서브클래스에 위임. (Spring 'BeanFactory'의 Factory 개념.)
 *
 * 상속 기반 분리의 한계(-> 8.4에서 인터페이스+합성으로 해결):
 *   ① 단일 상속 제약: 부모를 하나만 가질 수 있어, 다른 클래스를 상속해야 하면 막힌다(SingleInheritanceLimitation 참고).
 *   ② 컴파일 타임 결합: "어떤 연결을 쓸지"가 'new NUserDao()'처럼 코드에 박혀, 바꾸려면 코드 수정+재컴파일.
 *   ③ 부모-자식 강결합: 부모(AbstractUserDao)가 바뀌면 모든 자식이 영향을 받는다.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("[8.3] 8.2 구조를 패턴으로 분석 + 한계");
        System.out.println();

        // 같은 부모 흐름(템플릿 메소드)을, 서브클래스가 만든 Connection(팩토리 메소드)으로 실행한다.
        AbstractUserDao dao = new NUserDao();   // ★ 한계②: '어떤 구현'인지가 이 줄에 박힘(컴파일 타임 결합)
        dao.createTable();
        dao.add(new User("p1", "패턴분석"));
        System.out.println("  실행 결과 = " + dao.get("p1"));
        System.out.println("  -> add/get 흐름(부모) = 템플릿 메소드, getConnection(자식) = 팩토리 메소드.");
        System.out.println();

        // 한계②를 눈으로: N -> D로 바꾸려면 위 'new NUserDao()'를 'new DUserDao()'로 '코드를 고쳐야' 한다.
        AbstractUserDao swapped = new DUserDao();   // 교체하려면 결국 소스를 수정 = 컴파일 타임 결합
        swapped.createTable();
        swapped.add(new User("p2", "교체하려면코드수정"));
        System.out.println("  [한계②] N->D 교체에 'new'를 바꿔야 했다(코드 수정+재컴파일 필요): " + swapped.get("p2"));

        System.out.println();
        System.out.println("=> 8.2 구조 = 템플릿 메소드 + 팩토리 메소드 패턴. 하지만 상속이라 ①단일상속 ②컴파일타임결합");
        System.out.println("   ③부모-자식 강결합의 한계가 있다. -> 8.4: 상속 대신 인터페이스+합성으로 전환.");
    }
}
