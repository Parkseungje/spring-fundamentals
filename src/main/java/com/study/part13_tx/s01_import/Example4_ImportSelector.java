package com.study.part13_tx.s01_import;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * [13.1] 예제4 — ImportSelector: 등록할 설정을 '런타임에 동적으로' 고른다(Spring Boot 자동설정의 원리).
 *
 * @Import(설정클래스)는 '정해진' 설정을 등록한다. 반면 @Import(ImportSelector 구현)는 그 ImportSelector가
 * '등록할 설정 클래스 이름(문자열 배열)'을 런타임에 '계산해서' 반환하고, 스프링은 그 이름들을 import한다.
 * 즉 조건(환경 변수, 프로파일, 클래스 존재 여부 등)에 따라 '다른 설정을 골라' 끼울 수 있다.
 *
 * 이 예제는 시스템 프로퍼티 'fruit' 값에 따라 AppleConfig 또는 BananaConfig를 동적으로 import한다.
 * Spring Boot의 @EnableAutoConfiguration도 이 방식(AutoConfigurationImportSelector)으로, 클래스패스에
 * 무엇이 있느냐(@ConditionalOnClass 등)에 따라 수많은 설정을 조건부로 import해 '자동 설정'을 완성한다.
 */
public class Example4_ImportSelector {

    // ImportSelector: selectImports가 반환하는 '클래스 이름들'이 곧 @Import 대상이 된다.
    static class FruitImportSelector implements ImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            String fruit = System.getProperty("fruit", "apple"); // 조건(여기선 시스템 프로퍼티)
            if ("banana".equals(fruit)) {
                return new String[]{ Configs.BananaConfig.class.getName() };  // 동적으로 BananaConfig 선택
            }
            return new String[]{ Configs.AppleConfig.class.getName() };       // 기본은 AppleConfig
        }
    }

    @Configuration
    @Import(FruitImportSelector.class)   // 설정 클래스가 아니라 '셀렉터'를 import -> 셀렉터가 동적으로 결정
    static class MainConfig {
    }

    public static void main(String[] args) {
        System.out.println("== [ImportSelector] 조건에 따라 설정을 동적으로 import ==");
        System.out.println("현재 fruit 프로퍼티 = " + System.getProperty("fruit", "apple") + " (없으면 apple 기본)");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        // 셀렉터가 고른 설정에 따라 둘 중 하나만 등록돼 있다.
        boolean hasApple = ctx.getBeanNamesForType(AppleService.class).length > 0;
        boolean hasBanana = ctx.getBeanNamesForType(BananaService.class).length > 0;
        System.out.println("AppleService 등록? " + hasApple);
        System.out.println("BananaService 등록? " + hasBanana);

        ctx.close();
        System.out.println("\n=> @Import는 '정해진 설정'뿐 아니라 ImportSelector로 '이름을 동적으로 반환'해 조건부 등록도 한다.");
        System.out.println("   (-Dfruit=banana 로 실행하면 BananaConfig가 대신 등록된다.)");
        System.out.println("   Spring Boot @EnableAutoConfiguration도 이 방식으로 클래스패스 조건에 맞는 설정을 자동 import한다.");
    }
}
