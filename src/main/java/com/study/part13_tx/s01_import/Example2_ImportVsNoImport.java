package com.study.part13_tx.s01_import;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * [13.1] 예제2 — @Import가 '있을 때 vs 없을 때'. 명시적 등록의 의미.
 *
 * @ComponentScan은 패키지를 훑어 자동으로 빈을 찾는다. 하지만 외부 라이브러리의 @Configuration이나
 * 스캔 대상 밖의 설정은 자동으로 잡히지 않는다. 그럴 때 @Import로 '명시적으로' 끌어와야 한다.
 *
 * 이 예제는 컴포넌트 스캔을 일부러 쓰지 않고(루트 설정만 등록), AppleConfig를:
 *  - Config_NoImport: @Import 안 함 -> AppleService 빈 없음 -> getBean 시 예외(재현)
 *  - Config_WithImport: @Import 함 -> AppleService 빈 등록됨(해결)
 * 으로 비교해 "@Import가 없으면 그 설정의 빈은 컨테이너에 없다"를 드러낸다.
 *
 * 예제1과의 차이: 예제1은 '결합되면 다 등록된다'가 초점, 예제2는 '@Import가 없으면 등록 안 된다(명시성)'가 초점.
 */
public class Example2_ImportVsNoImport {

    @Configuration
    static class Config_NoImport { // AppleConfig를 import하지 않음
    }

    @Configuration
    @Import(Configs.AppleConfig.class)
    static class Config_WithImport {
    }

    public static void main(String[] args) {
        System.out.println("== [@Import 없음] AppleConfig 미등록 -> 빈 없음 ==");
        AnnotationConfigApplicationContext noImport = new AnnotationConfigApplicationContext(Config_NoImport.class);
        try {
            noImport.getBean(AppleService.class);
            System.out.println("  (도달하면 안 됨)");
        } catch (NoSuchBeanDefinitionException e) {
            System.out.println("  NoSuchBeanDefinitionException -> AppleService 빈이 컨테이너에 없다");
        }
        noImport.close();

        System.out.println("\n== [@Import 있음] AppleConfig 등록 -> 빈 있음 ==");
        AnnotationConfigApplicationContext withImport = new AnnotationConfigApplicationContext(Config_WithImport.class);
        AppleService apple = withImport.getBean(AppleService.class);
        System.out.println("  apple = " + apple.hello());
        withImport.close();

        System.out.println("\n=> @ComponentScan(자동)과 달리 @Import는 '명시적'이다. 스캔 대상 밖 설정은 @Import해야 등록된다.");
    }
}
