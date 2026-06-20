package com.study.part13_tx.s01_import;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [13.1] 결합 대상이 되는 개별 설정 클래스들.
 *
 * 각 @Configuration이 빈을 하나씩 제공한다. 이들을 @Import로 한 설정에 모으는 것이 13.1의 주제다.
 * 일부러 @Component 스캔에 의존하지 않고 '명시적으로' @Import해서 등록하는 방식을 본다(외부 라이브러리
 * 설정처럼 스캔 대상 밖인 경우를 가정).
 */
public class Configs {

    @Configuration
    public static class AppleConfig {
        @Bean
        AppleService appleService() {
            return new AppleService();
        }
    }

    @Configuration
    public static class BananaConfig {
        @Bean
        BananaService bananaService() {
            return new BananaService();
        }
    }

    @Configuration
    public static class GreetingConfig {
        @Bean
        GreetingService greetingService() {
            return new GreetingService();
        }
    }
}
