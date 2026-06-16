package com.study.part12_aop.s01_aop_motivation;

/**
 * [12.1 데모] 로그는 잘 찍힌다. 문제는 '그 로그 코드가 모든 메서드에 박혀 있다'는 것.
 *
 * 출력만 보면 멀쩡하지만, OrderService.java를 열어보면 begin/end/exception + try-catch가 메서드마다
 * 똑같이 반복된다. 비즈니스 로직보다 부가(로그) 코드가 더 길다. 이 흩어진 관심사를 한 곳으로 모으는 게
 * AOP의 목표다(이후 소단원에서 프록시 -> @Aspect로 발전).
 */
public class Main {
    public static void main(String[] args) {
        OrderService service = new OrderService(new LogTracer());

        System.out.println("== 정상 주문 ==");
        service.order("노트북");

        System.out.println("\n== 검증 실패(예외) 주문 ==");
        try {
            service.order("");
        } catch (Exception ignored) {}

        System.out.println("\n=> 로그는 잘 찍히지만, begin/end/exception+try-catch가 '모든 메서드'에 흩어져 박혀 있다.");
        System.out.println("   비즈니스 로직보다 부가 코드가 더 길다(흩어진 관심사). 100개 메서드면 100군데 복붙.");
        System.out.println("   -> 이 부가 관심사를 한 곳으로 분리하는 것이 AOP(12.4 프록시 ~ 12.9 @Aspect).");
    }
}
