package com.study.part12_aop.s06_dynamic_proxy;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Proxy;

/**
 * [12.6] 예제4 — ★ JDK 동적 프록시의 함정: 구체 클래스 타입으로 캐스팅하면 ClassCastException.
 *
 * JDK 동적 프록시가 만든 프록시($ProxyN)는 '인터페이스(OrderService)를 구현'한 객체일 뿐, 진짜 클래스
 * (RealOrderService)를 '상속'한 게 아니다. 그래서 인터페이스 타입으로는 받을 수 있어도, 구체 클래스
 * 타입으로 캐스팅/주입하려 하면 ClassCastException이 난다.
 *   - 이것이 12.8에서 getBean(ScannedOrderService.class)가 실패했던 이유이자,
 *   - Spring Boot 3.x가 'CGLIB를 기본'으로 삼은 실질적 이유다(CGLIB는 구체 클래스를 상속하므로 구체 타입으로도 받힌다).
 *
 * 이 예제는 같은 진짜 객체를 (A) JDK 프록시 / (B) CGLIB 프록시로 감싸, 구체 타입 캐스팅이 각각 실패/성공함을 대조한다.
 */
public class Example4_JdkProxyCastingTrap {

    public static void main(String[] args) {
        RealOrderService real = new RealOrderService();

        // (A) JDK 동적 프록시: 인터페이스(OrderService)를 구현한 $ProxyN
        OrderService jdkProxy = (OrderService) Proxy.newProxyInstance(
                OrderService.class.getClassLoader(),
                new Class[]{OrderService.class},
                (proxy, method, mArgs) -> method.invoke(real, mArgs));

        System.out.println("== (A) JDK 동적 프록시 ==");
        System.out.println("  실제 클래스 = " + jdkProxy.getClass().getName());
        System.out.println("  OrderService(인터페이스) 타입으로 받기 -> OK");
        // 구체 클래스(RealOrderService)로 캐스팅 시도 -> 상속이 아니라 인터페이스 구현이라 실패
        try {
            RealOrderService asConcrete = (RealOrderService) jdkProxy;  // ClassCastException 발생
            System.out.println("  (도달 못 함) " + asConcrete);
        } catch (ClassCastException e) {
            System.out.println("  RealOrderService(구체 클래스)로 캐스팅 -> ClassCastException! (JDK 프록시는 구체 타입이 아님)");
        }

        // (B) CGLIB 프록시: 구체 클래스(RealOrderService)를 상속한 $$SpringCGLIB$$
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(RealOrderService.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, mArgs, proxy) -> proxy.invokeSuper(obj, mArgs));
        RealOrderService cglibProxy = (RealOrderService) enhancer.create();  // 구체 타입으로 바로 받힌다

        System.out.println("\n== (B) CGLIB 프록시 ==");
        System.out.println("  실제 클래스 = " + cglibProxy.getClass().getName());
        System.out.println("  RealOrderService(구체 클래스) 타입으로 받기 -> OK (구체 클래스를 상속했으므로)");

        System.out.println("\n=> JDK 프록시는 인터페이스 타입이라 구체 클래스로 캐스팅 시 ClassCastException.");
        System.out.println("   CGLIB는 구체 클래스를 상속해 구체 타입으로도 받힌다 -> Spring Boot 3.x가 CGLIB를 기본으로 삼은 실질적 이유.");
    }
}
