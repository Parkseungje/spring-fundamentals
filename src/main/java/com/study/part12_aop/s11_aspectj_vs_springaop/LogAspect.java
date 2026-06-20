package com.study.part12_aop.s11_aspectj_vs_springaop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * [12.11] external() 메서드에 로그를 붙이는 애스펙트. 내부 호출 시 이 로그가 '안' 붙는 게 핵심 관전 포인트.
 */
@Aspect
public class LogAspect {

    @Around("execution(* com.study.part12_aop.s11_aspectj_vs_springaop.CallService.external(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("  [AOP] external 시작 <<< 어드바이스 적용됨");
        Object result = pjp.proceed();
        System.out.println("  [AOP] external 종료");
        return result;
    }
}
