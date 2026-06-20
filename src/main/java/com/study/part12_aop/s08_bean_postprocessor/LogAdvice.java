package com.study.part12_aop.s08_bean_postprocessor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * [12.8] Advice — 부가 로직(로그). 12.7과 동일한 표준 Advice(org.aopalliance MethodInterceptor).
 */
public class LogAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String name = invocation.getMethod().getName();
        System.out.println("--> [" + name + "] 시작");
        Object result = invocation.proceed();
        System.out.println("<-- [" + name + "] 종료");
        return result;
    }
}
