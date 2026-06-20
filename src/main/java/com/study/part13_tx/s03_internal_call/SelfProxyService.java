package com.study.part13_tx.s03_internal_call;

import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.3] 해결책(2) 시연용 — AopContext.currentProxy()로 '내부 호출도 프록시를 거치게' 만든다.
 *
 * 13.3의 함정은 external()이 this.internal()을 부르면 프록시를 우회하는 것이었다. 여기서는 this 대신
 * '현재 프록시'를 얻어 그 프록시의 internal()을 부른다. 그러면 호출이 프록시를 거쳐 @Transactional이 적용된다.
 *
 *   - AopContext.currentProxy() : 지금 실행 중인 빈의 '프록시'를 돌려준다(this가 아니라 프록시).
 *   - 단, @EnableAspectJAutoProxy(exposeProxy = true)로 '프록시를 노출'하도록 켜야 currentProxy()가 동작한다.
 *
 * 권장도는 낮다(코드가 지저분하고 의도가 잘 안 드러남). 가장 권장되는 해결은 '별도 빈 분리'(Example3)다.
 * 이 예제는 "내부 호출이라도 프록시로 돌리면 트랜잭션이 걸린다"는 원리를 코드로 증명하기 위한 것이다.
 */
public class SelfProxyService {

    @Transactional
    public void internal() {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("    internal() 실행 -> 트랜잭션 활성? " + active
                + (active ? "  (true = 프록시 경유라 적용됨!)" : "  (false)"));
    }

    public void externalViaProxy() {
        System.out.println("    externalViaProxy() -> this 대신 '현재 프록시'로 internal() 호출:");
        // this.internal()(우회) 대신, 현재 프록시를 얻어 프록시.internal()을 부른다 -> 프록시 경유 -> @Transactional 적용
        SelfProxyService proxy = (SelfProxyService) AopContext.currentProxy();
        proxy.internal();
    }
}
