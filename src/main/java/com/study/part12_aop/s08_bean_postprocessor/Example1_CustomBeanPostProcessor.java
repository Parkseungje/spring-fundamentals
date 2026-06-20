package com.study.part12_aop.s08_bean_postprocessor;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [12.8] 예제1 — 빈 후처리기(BeanPostProcessor)란? 빈을 등록 직전에 가로채 '프록시로 바꿔치기'한다.
 *
 * 12.7의 한계: 빈마다 ProxyFactory로 프록시를 손수 만들어야 했다(빈 100개면 설정 100번). 또 클라이언트가
 * ctx.getBean(OrderService.class)로 꺼내면 '원본'이 나와 프록시가 적용되지 않는다.
 *
 * BeanPostProcessor는 스프링 컨테이너가 빈을 만들고 '등록하기 직전에' 가로채는 후크다.
 * postProcessAfterInitialization에서 '다른 객체를 반환하면' 그게 진짜 빈으로 등록된다 -> 여기서 원본 대신
 * 프록시를 반환하면, 컨테이너에는 프록시가 등록되고 getBean도 프록시를 돌려준다(원본 바꿔치기).
 *
 * 가설: 커스텀 후처리기를 등록하면, @Bean으로 등록한 RealOrderService가 컨테이너 안에서 프록시로 바뀌어
 * getBean 시 프록시(SpringCGLIB/$Proxy)가 나오고 호출에 로그가 붙는다.
 */
public class Example1_CustomBeanPostProcessor {

    // 빈 후처리기: OrderService 타입 빈을 LogProxy로 감싸 '바꿔치기'한다.
    static class LogProxyBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof OrderService) {
                ProxyFactory pf = new ProxyFactory(bean); // 원본을 target으로
                pf.addAdvice(new LogAdvice());
                Object proxy = pf.getProxy();
                System.out.println("    [후처리기] 빈 '" + beanName + "'(" + bean.getClass().getSimpleName()
                        + ")을 프록시로 교체");
                return proxy; // 원본 대신 프록시를 등록
            }
            return bean;
        }
    }

    @Configuration
    static class Config {
        @Bean
        OrderService orderService() {
            return new RealOrderService();
        }

        @Bean
        LogProxyBeanPostProcessor logProxyBeanPostProcessor() {
            return new LogProxyBeanPostProcessor();
        }
    }

    public static void main(String[] args) {
        System.out.println("== [빈 후처리기] 컨테이너가 빈을 프록시로 바꿔치기 ==");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

        OrderService bean = ctx.getBean(OrderService.class);
        System.out.println("getBean으로 꺼낸 실제 클래스 = " + bean.getClass().getName()); // 프록시여야 함
        bean.order("노트북");

        ctx.close();
        System.out.println("\n=> @Bean은 RealOrderService를 반환했지만, 후처리기가 프록시로 교체해 컨테이너엔 프록시가 등록됐다.");
        System.out.println("   getBean도 프록시를 돌려주고 호출에 로그가 붙는다. 단 모든 빈을 손수 if로 거르는 건 여전히 번거롭다 -> 예제2.");
    }
}
