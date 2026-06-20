package com.study.part13_tx.s01_import;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.1] 예제1 — @Import로 여러 @Configuration을 한곳에서 결합한다.
 *
 * @Import({A.class, B.class})는 여러 설정 클래스를 하나의 설정으로 묶는다. 컨테이너를 띄울 때 루트 설정
 * 하나(MainConfig)만 넘겨도, @Import된 AppleConfig/BananaConfig의 빈까지 전부 등록된다.
 *
 * 가설: MainConfig만으로 컨텍스트를 만들어도 AppleService와 BananaService 빈을 모두 꺼낼 수 있다.
 */
public class Example1_ImportCombine {

    @Configuration
    @Import({Configs.AppleConfig.class, Configs.BananaConfig.class}) // 두 설정을 명시적으로 결합
    static class MainConfig {
    }

    public static void main(String[] args) {
        System.out.println("== [@Import] 여러 설정을 한곳에서 결합 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        AppleService apple = ctx.getBean(AppleService.class);
        BananaService banana = ctx.getBean(BananaService.class);
        System.out.println("apple  = " + apple.hello());
        System.out.println("banana = " + banana.hello());

        ctx.close();
        System.out.println("\n=> 루트 설정(MainConfig) 하나만 넘겼는데 @Import된 두 설정의 빈이 모두 등록됐다.");
    }
}
