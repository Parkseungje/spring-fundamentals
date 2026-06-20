package com.study.part13_tx.s03_internal_call;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.3] 내부 호출 함정 재현용 서비스 — 한 클래스 안에 external()과 @Transactional internal()을 둔다.
 *
 * 핵심 관찰 도구: TransactionSynchronizationManager.isActualTransactionActive()
 *   - "지금 이 메서드가 '진짜 트랜잭션' 안에서 실행되고 있는가?"를 true/false로 알려준다.
 *   - @Transactional이 제대로 걸리면(프록시를 거쳤으면) true, 안 걸렸으면 false.
 *   => 이걸로 "@Transactional이 실제로 적용됐는지"를 눈으로 확인한다.
 *
 * 시나리오:
 *   - internal(): @Transactional. 프록시를 거쳐 호출되면 트랜잭션이 시작되어 active=true 여야 한다.
 *   - external(): @Transactional 없음. 내부에서 this.internal()을 부른다. 이때 this는 프록시가 아니라
 *     '진짜 객체(target)'라서 internal()의 @Transactional이 무시된다(active=false) -> 이것이 함정.
 */
public class TxService {

    @Transactional
    public void internal() {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("    internal() 실행 -> 트랜잭션 활성? " + active
                + (active ? "  (true = @Transactional 적용됨)" : "  (false = @Transactional 무시됨!)"));
    }

    // @Transactional 없음. 내부에서 this.internal()을 호출한다 -> 프록시 우회.
    public void external() {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("    external() 실행 -> 트랜잭션 활성? " + active + " (external 자체는 @Transactional 없음)");
        System.out.println("    -> 내부에서 this.internal() 호출:");
        internal(); // = this.internal() : 프록시가 아니라 target에서 직접 호출 -> @Transactional 무시됨
    }
}
