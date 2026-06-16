package com.study.part12_aop.s06_dynamic_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * [12.6] 예제2 — JDK 동적 프록시: InvocationHandler 하나로 프록시 클래스를 '런타임 자동 생성'.
 *
 * 수동 프록시(12.4)의 고통: 대상이 100개면 거의 같은 프록시 클래스 100개를 손으로 써야 했다.
 * JDK 동적 프록시는 Reflection(예제1) 위에서, 'InvocationHandler' 하나만 만들면 자바가 프록시 클래스를
 * 런타임에 자동으로 찍어낸다($Proxy0, $Proxy1...). 그래서 구현 대상이 몇 개든 '핸들러 1개'면 된다.
 *
 * 동작 원리:
 *  - Proxy.newProxyInstance(클래스로더, 구현할 인터페이스들, 핸들러)가 그 인터페이스를 구현한 프록시 객체를 생성.
 *  - 클라이언트가 프록시의 메서드를 부르면 -> 자바가 핸들러의 invoke(proxy, method, args)로 모든 호출을 보냄.
 *  - 핸들러는 부가 기능(로그)을 한 뒤 method.invoke(target, args)로 진짜 객체에 위임(예제1의 리플렉션 그대로).
 *
 * 전제: '인터페이스가 있어야' 한다(인터페이스를 구현하는 방식이라). 인터페이스가 없으면 CGLIB(예제3).
 */
public class Example2_JdkDynamicProxy {

    // 핵심: 부가 기능 핸들러는 '딱 하나'. 어떤 인터페이스/구현체든 이 하나로 감싼다.
    static class LogInvocationHandler implements InvocationHandler {
        private final Object target; // 진짜 객체(어떤 타입이든)

        LogInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long start = System.currentTimeMillis();
            System.out.println("--> [" + method.getName() + "] 시작");
            Object result = method.invoke(target, args); // 진짜 객체에 위임(리플렉션)
            System.out.println("<-- [" + method.getName() + "] 종료 (" + (System.currentTimeMillis() - start) + "ms)");
            return result;
        }
    }

    // 제네릭 헬퍼: 어떤 인터페이스 타입이든 같은 핸들러로 프록시를 만들어 준다.
    @SuppressWarnings("unchecked")
    static <T> T createProxy(Class<T> interfaceType, Object target) {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class[]{interfaceType},          // 프록시가 구현할 인터페이스
                new LogInvocationHandler(target));   // 핸들러 하나
    }

    public static void main(String[] args) {
        System.out.println("== [JDK 동적 프록시] 핸들러 하나로 서로 다른 인터페이스 프록시 자동 생성 ==");

        // 같은 LogInvocationHandler/createProxy로 '서로 다른' 두 인터페이스의 프록시를 만든다.
        OrderService orderProxy = createProxy(OrderService.class, new RealOrderService());
        MemberService memberProxy = createProxy(MemberService.class, new RealMemberService());

        // 클라이언트는 평범하게 인터페이스 메서드를 부른다 -> 모두 핸들러의 invoke로 흘러 로그가 붙는다.
        System.out.println("[OrderService 프록시]");
        System.out.println("  결과 = " + orderProxy.order("노트북"));
        System.out.println("[MemberService 프록시]");
        memberProxy.join("홍길동");

        // 프록시의 실제 클래스 이름을 확인 -> 자바가 런타임에 만든 $ProxyN.
        System.out.println("\n프록시 실제 클래스:");
        System.out.println("  orderProxy  = " + orderProxy.getClass().getName());   // 예: com.sun.proxy.$Proxy0
        System.out.println("  memberProxy = " + memberProxy.getClass().getName());  // 예: com.sun.proxy.$Proxy1

        System.out.println("\n=> 프록시 클래스를 손으로 안 만들고 핸들러 1개로 자동 생성($ProxyN). 구현체가 100개여도 핸들러 1개.");
        System.out.println("   단 '인터페이스 필수'. 인터페이스 없는 구체 클래스는 CGLIB로(예제3).");
    }
}
