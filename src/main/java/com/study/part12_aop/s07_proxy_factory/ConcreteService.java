package com.study.part12_aop.s07_proxy_factory;

/**
 * [12.7] 인터페이스가 '없는' 구체 클래스 — ProxyFactory가 CGLIB를 선택하는 대상.
 * Example1에서 "인터페이스 없으면 ProxyFactory가 자동으로 CGLIB를 고른다"를 보여주는 데 쓴다.
 */
public class ConcreteService {

    public String run() {
        System.out.println("    [비즈니스] 구체 클래스 작업 실행");
        return "실행완료";
    }
}
