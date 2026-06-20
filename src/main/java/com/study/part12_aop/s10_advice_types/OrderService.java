package com.study.part12_aop.s10_advice_types;

/**
 * [12.10] 어드바이스 5종을 적용할 서비스. order(정상), fail(예외)로 정상/예외 흐름을 구분한다.
 */
public interface OrderService {

    String order(String item);

    String fail(String item); // 일부러 예외를 던지는 메서드(@AfterThrowing 시연용)
}
