package com.study.part12_aop.s07_proxy_factory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * [12.7] Advice — '어떤 부가 로직'을 적용할지(여기선 로그/시간 측정).
 *
 * 12.6에서 JDK는 InvocationHandler, CGLIB는 (스프링)MethodInterceptor로 핸들러 API가 '서로 달랐다'.
 * 스프링 ProxyFactory는 이를 'Advice'라는 하나의 추상으로 통합한다. 개발자는 표준 인터페이스
 * org.aopalliance.intercept.MethodInterceptor 하나만 구현하면 되고, 대상이 JDK로 감싸지든 CGLIB로
 * 감싸지든 '이 Advice 하나'가 그대로 동작한다(통합의 핵심).
 *
 * 주의: 같은 이름의 'MethodInterceptor'가 두 개 있다.
 *  - org.aopalliance.intercept.MethodInterceptor  <- 이게 스프링 Advice 표준(지금 구현하는 것)
 *  - org.springframework.cglib.proxy.MethodInterceptor <- 12.6에서 CGLIB 직접 쓸 때의 저수준 것
 * 헷갈리지 말 것. ProxyFactory를 쓰면 저수준은 스프링이 처리하고 우리는 aopalliance 쪽만 작성한다.
 *
 * invocation.proceed(): '진짜 메서드(또는 다음 어드바이스)'를 호출한다. 이 앞뒤를 감싸 부가 기능을 넣는다.
 * (12.3 템플릿 콜백에서 콜백을 실행하던 자리와 같은 역할 — 변하지 않는 흐름 사이의 '변하는 실행 지점'.)
 */
public class LogAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String name = invocation.getMethod().getName();
        long start = System.currentTimeMillis();
        System.out.println("--> [" + name + "] 시작");
        Object result = invocation.proceed(); // 진짜 메서드 호출(위임). JDK/CGLIB 무관하게 동일하게 동작.
        System.out.println("<-- [" + name + "] 종료 (" + (System.currentTimeMillis() - start) + "ms)");
        return result;
    }
}
