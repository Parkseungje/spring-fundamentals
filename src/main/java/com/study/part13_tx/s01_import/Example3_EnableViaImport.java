package com.study.part13_tx.s01_import;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [13.1] 예제3 — @EnableXxx의 정체: 내부적으로 @Import를 쓴다.
 *
 * @EnableAspectJAutoProxy(12.8), @EnableTransactionManagement(PART 13), @EnableScheduling 등 수많은
 * @EnableXxx는 마법이 아니라, '필요한 설정 클래스를 @Import하는 메타 애노테이션'일 뿐이다.
 *
 * 이 예제는 그 구조를 직접 흉내 낸다: 커스텀 @EnableGreeting 애노테이션에 @Import(GreetingConfig)를 붙여
 * 두고, 설정 클래스에 @EnableGreeting만 달면 GreetingConfig가 import되어 GreetingService 빈이 등록된다.
 * 즉 @EnableGreeting = "GreetingConfig를 @Import해줘"의 별명이다.
 *
 * 예제1·2와의 차이: 직접 @Import를 쓰는 대신, @Import를 품은 '메타 애노테이션(@EnableXxx)'을 만들어
 * 스프링의 @EnableXxx 동작 원리를 드러낸다.
 */
public class Example3_EnableViaImport {

    // 커스텀 @EnableGreeting: 내부에 @Import(GreetingConfig)를 품는다 -> 이게 @EnableXxx의 원리.
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Import(Configs.GreetingConfig.class)
    @interface EnableGreeting {
    }

    @Configuration
    @EnableGreeting // 이 한 줄이 GreetingConfig를 import한다(직접 @Import 안 써도 됨)
    static class MainConfig {
    }

    public static void main(String[] args) {
        System.out.println("== [@EnableXxx 원리] 메타 애노테이션이 @Import를 품는다 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        GreetingService greeting = ctx.getBean(GreetingService.class);
        System.out.println("greeting = " + greeting.hello());

        ctx.close();
        System.out.println("\n=> @EnableGreeting만 달았는데 GreetingService가 등록됐다. @EnableXxx = '설정을 @Import하는 별명'.");
        System.out.println("   스프링의 @EnableAspectJAutoProxy/@EnableTransactionManagement도 같은 방식으로 동작한다.");
    }
}
