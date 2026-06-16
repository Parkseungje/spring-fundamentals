package com.study.part12_aop.s01_aop_motivation;

import com.study.part12_aop.s01_aop_motivation.LogTracer.TraceStatus;

/**
 * [12.1] "흩어진 관심사"의 고통을 코드로 보여주는 서비스.
 *
 * ★ 핵심 관찰: 아래 order()와 validate() 두 메서드를 보면, '비즈니스 로직'(주문 처리, 검증)은 한두 줄인데
 * 그 주위를 똑같은 '로그 코드'(begin/end/exception + try-catch)가 매번 감싸고 있다. 메서드가 100개면
 * 이 6~7줄짜리 똑같은 패턴을 100군데 복붙해야 한다 = "배보다 배꼽". 이것이 '흩어진 관심사
 * (cross-cutting concern)'다 — 로깅이라는 부가 관심사가 여러 메서드에 흩뿌려져 있다.
 *
 * 변하지 않는 것(로그 시작/종료·시간 측정·예외 로깅) vs 변하는 것(비즈니스 로직)을 분리하고 싶다.
 * OOP만으로는 이 '흩어짐'을 깔끔히 못 없앤다 -> 이게 AOP가 풀려는 문제다(12.4~ 프록시, 12.9 @Aspect).
 */
public class OrderService {

    private final LogTracer tracer;

    public OrderService(LogTracer tracer) {
        this.tracer = tracer;
    }

    public void order(String item) {
        // --- 부가 관심사(로그) 시작 — 비즈니스와 무관한 보일러플레이트 ---
        TraceStatus status = tracer.begin("OrderService.order");
        try {
            // --- 여기부터 '진짜' 비즈니스 로직(핵심 관심사) ---
            validate(item);
            System.out.println("    [비즈니스] " + item + " 주문 처리");
            // --- 비즈니스 로직 끝 ---
            tracer.end(status);                 // 부가 관심사(로그) 종료
        } catch (Exception e) {
            tracer.exception(status, e);        // 부가 관심사(예외 로그)
            throw e;
        }
    }

    public void validate(String item) {
        // ★ 똑같은 로그 보일러플레이트가 '또' 반복된다(흩어진 관심사).
        TraceStatus status = tracer.begin("OrderService.validate");
        try {
            if (item == null || item.isBlank()) {
                throw new IllegalArgumentException("상품명이 비었습니다");
            }
            System.out.println("    [비즈니스] " + item + " 검증 통과");
            tracer.end(status);
        } catch (Exception e) {
            tracer.exception(status, e);
            throw e;
        }
    }
}
