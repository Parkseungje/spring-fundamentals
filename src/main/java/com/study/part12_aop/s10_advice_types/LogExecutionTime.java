package com.study.part12_aop.s10_advice_types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [12.10] 커스텀 어노테이션 — '@annotation' 포인트컷으로 'execution(메서드 패턴)' 대신 쓰는 방식.
 *
 * execution(* order(..))은 '메서드 이름/패턴'으로 대상을 고른다. 그런데 실무에선 "내가 콕 집어 표시한
 * 메서드에만" 부가 기능을 걸고 싶을 때가 많다. 그때 이런 커스텀 어노테이션을 만들고, 포인트컷을
 * @annotation(...)으로 잡으면 "이 어노테이션이 붙은 메서드"에만 어드바이스가 적용된다.
 *
 * RetentionPolicy.RUNTIME 필수: 런타임에 리플렉션으로 어노테이션을 읽어야 AOP가 인식한다(SOURCE/CLASS면 못 봄).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
}
