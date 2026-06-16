package com.study.part12_aop.s06_dynamic_proxy;

import java.lang.reflect.Method;

/**
 * [12.6] 예제1 — Reflection(리플렉션): 동적 프록시의 '출발점'. 어떤 메서드든 이름으로 찾아 실행한다.
 *
 * 12.4의 수동 프록시는 인터페이스 메서드를 '컴파일 시점에' 일일이 구현해야 했다. 동적 프록시는 그걸
 * 런타임에 자동화하는데, 그 기반 기술이 Reflection이다. Reflection은 클래스/메서드 정보를 런타임에 들여다보고
 * (introspection), method.invoke(target, args)로 '메서드 이름만 알면' 어떤 메서드든 동적으로 호출한다.
 *
 * 핵심 이점: 대상이 무엇이든, 메서드가 무엇이든 '하나의 일반 코드'로 호출을 가로채 부가 기능(로그/시간)을
 * 끼울 수 있다 -> 메서드마다 위임 코드를 쓸 필요가 사라진다(수동 프록시 폭발 문제의 해결 실마리).
 *
 * 한계(이 예제가 같이 보여줌): 메서드를 '문자열 이름'으로 다루므로 '컴파일 시점 오류 검출이 안 된다'.
 * 오타("oder")를 내도 컴파일은 통과하고 '실행 중에야' 터진다(NoSuchMethodException). 그래서 Reflection은
 * 일반 비즈니스 코드용이 아니라 '프레임워크 개발용'이다(스프링 내부가 이걸 쓴다).
 */
public class Example1_Reflection {

    // 어떤 target의 어떤 메서드든 '일반적으로' 가로채 로그를 더해 실행하는 코드(리플렉션).
    static Object invokeWithLog(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, paramTypes); // 이름으로 메서드를 '찾는다'
        long start = System.currentTimeMillis();
        System.out.println("--> [" + target.getClass().getSimpleName() + "." + methodName + "] 시작");
        Object result = method.invoke(target, args);                         // 동적 호출(어떤 메서드든)
        System.out.println("<-- [" + methodName + "] 종료 (" + (System.currentTimeMillis() - start) + "ms)");
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== [Reflection] method.invoke로 어떤 메서드든 동적 호출 ==");

        RealOrderService order = new RealOrderService();
        RealMemberService member = new RealMemberService();

        // 같은 invokeWithLog 코드로 '서로 다른 클래스의 다른 메서드'를 가로채 실행 -> 일반화의 힘.
        Object r = invokeWithLog(order, "order", new Class[]{String.class}, "노트북");
        System.out.println("    결과 = " + r);
        invokeWithLog(member, "join", new Class[]{String.class}, "홍길동");

        // 한계 시연: 메서드 이름을 문자열로 다루므로 오타가 '런타임에야' 터진다(컴파일은 통과).
        System.out.println("\n[한계] 존재하지 않는 메서드명 'oder'(오타) 호출 시도:");
        try {
            invokeWithLog(order, "oder", new Class[]{String.class}, "노트북");
        } catch (NoSuchMethodException e) {
            System.out.println("    -> NoSuchMethodException (컴파일 통과, 실행 중 발견). 그래서 Reflection은 프레임워크용.");
        }

        System.out.println("\n=> 이름만 알면 어떤 메서드든 동적 호출 가능(일반화). 단 컴파일 시점 안전성 상실 -> 다음은 이를");
        System.out.println("   더 안전·편리하게 감싼 'JDK 동적 프록시'(예제2).");
    }
}
