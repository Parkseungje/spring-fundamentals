package com.study.part12_aop.s08_bean_postprocessor;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [12.8] 예제4 — 빈 생명주기 순서: BPP.before -> @PostConstruct -> BPP.after.
 *        (자동 프록시 생성기가 '왜 after에서' 프록시를 교체하는지 + 13.4 @PostConstruct 함정의 근거)
 *
 * BeanPostProcessor에는 두 메서드가 있다:
 *   - postProcessBeforeInitialization : 초기화 콜백(@PostConstruct) '전'
 *   - postProcessAfterInitialization  : 초기화 콜백 '후'  <- 자동 프록시 생성기는 여기서 프록시로 교체한다
 *
 * 왜 after인가: @PostConstruct 같은 초기화까지 끝난 '완성된 원본'을 감싸야 하기 때문이다. 그래서 프록시는
 * 초기화 '후'에 만들어진다. 뒤집어 말하면, @PostConstruct가 실행되는 시점엔 '아직 프록시가 없다'
 * -> 13.4의 "@PostConstruct + @Transactional은 트랜잭션이 안 걸린다"의 직접적 근거가 바로 이 순서다.
 *
 * 이 예제는 커스텀 BPP가 before/after를 찍고, 빈의 @PostConstruct도 찍게 해서 호출 순서를 눈으로 확인한다.
 * (여기 BPP는 프록시로 바꾸지 않고 '순서만' 보여준다 — 프록시 교체 자체는 예제1~3에서 다뤘다.)
 */
public class Example4_BeanLifecycleOrder {

    static class MyBean {
        MyBean() {
            System.out.println("1) 생성자 (객체 생성)");
        }

        @PostConstruct
        public void init() {
            System.out.println("3) @PostConstruct (초기화) — 이 시점엔 '아직 프록시가 없다'(13.4 함정의 근거)");
        }
    }

    static class OrderTracingBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof MyBean) {
                System.out.println("2) BPP.before (초기화 '전')");
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof MyBean) {
                System.out.println("4) BPP.after (초기화 '후') — 자동 프록시 생성기라면 '여기서' 프록시로 교체");
            }
            return bean;
        }
    }

    @Configuration
    static class Config {
        @Bean MyBean myBean() { return new MyBean(); }
        @Bean OrderTracingBeanPostProcessor tracingBpp() { return new OrderTracingBeanPostProcessor(); }
    }

    public static void main(String[] args) {
        System.out.println("== 빈 생명주기 순서: 생성 -> BPP.before -> @PostConstruct -> BPP.after ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        ctx.close();
        System.out.println("\n=> 순서: 생성(1) -> BPP.before(2) -> @PostConstruct(3) -> BPP.after(4).");
        System.out.println("   프록시 교체는 (4)after에서 일어난다 -> @PostConstruct(3) 시점엔 프록시가 없다(13.4 함정의 뿌리).");
    }
}
