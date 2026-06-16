package com.study.part12_aop.s06_dynamic_proxy;

/**
 * [12.6] 진짜 객체 2 — MemberService 구현. Example2에서 OrderService와 같은 핸들러로 함께 감싼다.
 */
public class RealMemberService implements MemberService {

    @Override
    public void join(String name) {
        System.out.println("    [비즈니스] " + name + " 회원 가입 처리");
    }
}
