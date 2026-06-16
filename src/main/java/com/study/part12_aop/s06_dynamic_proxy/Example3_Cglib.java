package com.study.part12_aop.s06_dynamic_proxy;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * [12.6] 예제3 — CGLIB: 인터페이스 없이 '구체 클래스를 상속'해 프록시 생성(바이트코드 조작).
 *
 * JDK 동적 프록시(예제2)는 인터페이스가 있어야 했다. CGLIB는 인터페이스 없이도, 대상 '클래스를 상속한
 * 자식 클래스'를 런타임에 바이트코드로 만들어 프록시로 쓴다. 그래서 구체 클래스만 있어도 프록시가 된다.
 *  - Enhancer: 프록시(자식 클래스)를 만들어 주는 생성기.
 *  - MethodInterceptor: 모든 메서드 호출을 가로채는 콜백(JDK의 InvocationHandler에 대응).
 *  - proxy.invokeSuper(obj, args): 부모(진짜 클래스)의 원래 메서드를 호출(= 위임).
 *
 * 한계: 상속으로 동작하므로 'final 클래스/메서드'는 프록시 불가(오버라이드가 안 되니까). 아래에서 final
 * 메서드 pay()에는 부가 로그가 '안 붙는' 것으로 이를 확인한다. -> PART 14 JPA 엔티티 final 금지의 이유.
 *
 * 참고: 여기서 import는 org.springframework.cglib (스프링이 CGLIB를 재패키징해 내장). Spring AOP가 인터페이스
 * 없는 빈을 프록시할 때 바로 이 CGLIB를 쓴다(Spring Boot 3.x 기본).
 */
public class Example3_Cglib {

    public static void main(String[] args) {
        System.out.println("== [CGLIB] 구체 클래스를 상속해 프록시 생성(인터페이스 불필요) ==");

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ConcreteOrderService.class); // 이 클래스를 '상속'한 프록시를 만든다
        enhancer.setCallback((MethodInterceptor) (obj, method, methodArgs, proxy) -> {
            // 모든 메서드 호출이 여기로 들어온다(부가 기능을 더한 뒤 부모 메서드에 위임).
            long start = System.currentTimeMillis();
            System.out.println("--> [" + method.getName() + "] 시작");
            Object result = proxy.invokeSuper(obj, methodArgs); // 부모(진짜) 메서드 호출 = 위임
            System.out.println("<-- [" + method.getName() + "] 종료 (" + (System.currentTimeMillis() - start) + "ms)");
            return result;
        });

        ConcreteOrderService proxy = (ConcreteOrderService) enhancer.create(); // 프록시(자식 인스턴스)

        System.out.println("[일반 메서드 order()] -> 프록시(자식)가 오버라이드 -> 로그 붙음");
        System.out.println("  결과 = " + proxy.order("노트북"));

        System.out.println("[final 메서드 pay()] -> 오버라이드 불가 -> 로그 안 붙음(부가 기능 미적용)");
        System.out.println("  결과 = " + proxy.pay("노트북"));

        System.out.println("\n프록시 실제 클래스 = " + proxy.getClass().getName()); // ...$$EnhancerBySpringCGLIB$$...

        System.out.println("\n=> 인터페이스 없이 구체 클래스를 상속해 프록시 생성. 단 final 메서드(pay)는 오버라이드 불가라");
        System.out.println("   로그가 안 붙었다(begin/end 없이 바로 비즈니스). 이것이 JPA 엔티티에 final을 금지하는 이유.");
    }

    // (참고용) 위 람다 콜백을 클래스로 풀어 쓰면 이런 모양이다 — MethodInterceptor 구현.
    static class LogMethodInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            System.out.println("--> [" + method.getName() + "] 시작");
            Object result = proxy.invokeSuper(obj, args);
            System.out.println("<-- [" + method.getName() + "] 종료");
            return result;
        }
    }
}
