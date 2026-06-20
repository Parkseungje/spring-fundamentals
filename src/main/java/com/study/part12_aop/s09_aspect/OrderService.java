package com.study.part12_aop.s09_aspect;

/**
 * [12.9] @Aspect 적용 대상 인터페이스. order(타겟)와 findStock(포인트컷 비대상)으로 구분.
 */
public interface OrderService {

    String order(String item);

    int findStock(String item);
}
