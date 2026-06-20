package com.study.part12_aop.s11_aspectj_vs_springaop;

/**
 * [12.11] 내부 호출 한계를 보여줄 서비스(구체 클래스 -> CGLIB 프록시).
 *
 * Spring AOP는 '프록시'를 거쳐야만 어드바이스가 동작한다. 그런데 한 메서드가 같은 객체의 다른 메서드를
 * this.method() 형태(내부 호출)로 부르면, 그 호출은 프록시를 거치지 않고 '진짜 객체(this)'에서 바로
 * 일어난다. 그래서 내부 호출 대상에는 어드바이스가 적용되지 않는다.
 *
 * - external(): 어드바이스 대상. 외부에서 프록시로 호출하면 어드바이스가 붙는다.
 * - internalCaller(): 내부에서 this.external()을 호출한다 -> 프록시를 우회 -> external에 어드바이스 미적용.
 *
 * 각 메서드에서 this.getClass()를 찍어, 메서드 안의 this가 '프록시'가 아니라 '진짜 객체'임을 드러낸다
 * (이것이 내부 호출이 프록시를 못 거치는 근본 원인).
 */
public class CallService {

    public void external() {
        System.out.println("    [비즈니스] external() 실행 (this=" + this.getClass().getSimpleName() + ")");
    }

    public void internalCaller() {
        System.out.println("    [비즈니스] internalCaller() 실행 (this=" + this.getClass().getSimpleName() + ")");
        System.out.println("    -> 내부에서 this.external() 호출:");
        external(); // = this.external() : 프록시가 아니라 진짜 객체에서 직접 호출됨
    }
}
