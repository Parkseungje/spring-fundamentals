package com.study.part12_aop.s08_bean_postprocessor;

/**
 * [12.8] 빈으로 등록될 서비스 인터페이스. order에만 Pointcut을 매칭시켜 findStock과 구분한다.
 */
public interface OrderService {

    String order(String item);

    int findStock(String item);
}
