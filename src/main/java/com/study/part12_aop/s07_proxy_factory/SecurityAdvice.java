package com.study.part12_aop.s07_proxy_factory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * [12.7] 두 번째 Advice — 어드바이저 체인 순서를 보여주기 위한 '보안' 부가 로직.
 *
 * Example4에서 LogAdvice와 함께 한 프록시에 둘 다 등록해, '프록시 1개 + 어드바이저 N개'가 등록 순서대로
 * 줄지어(proceed 체인) 도는 것을 보여준다. 바깥 어드바이스가 먼저 진입하고, 가장 안쪽이 진짜 메서드를 부른다.
 */
public class SecurityAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("  [보안] 권한 확인(진입)");
        Object result = invocation.proceed();   // 다음 어드바이스(또는 진짜 메서드)로
        System.out.println("  [보안] 정리(복귀)");
        return result;
    }
}
