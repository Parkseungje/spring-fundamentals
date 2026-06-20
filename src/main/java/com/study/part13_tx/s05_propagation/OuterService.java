package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import org.springframework.transaction.annotation.Transactional;

/**
 * [13.5] 외부 트랜잭션 — 내부(InnerService)를 호출하며 전파가 어떻게 동작하는지 보여준다.
 *
 * 외부는 모두 @Transactional(REQUIRED 기본). 내부 호출 방식·내부 전파 옵션·예외 처리 여부를 조합해
 * 전파의 핵심 시나리오 4가지를 만든다. (A는 외부, B는 inner가 건드린다.)
 */
public class OuterService {

    private final AccountDao dao;
    private final InnerService inner;

    public OuterService(AccountDao dao, InnerService inner) {
        this.dao = dao;
        this.inner = inner;
    }

    // (1) REQUIRED + 내부 정상: 외부 A-100, 내부 B+100이 '하나의 물리 트랜잭션'으로 함께 커밋
    @Transactional
    public void requiredInnerOk() {
        dao.addBalance("A", -100);
        inner.addOk();
        System.out.println("    [outer] 정상 종료 -> 커밋(A-100, B+100 함께)");
    }

    // (2) REQUIRED + 내부 실패(예외 전파): 같은 물리 트랜잭션이라 외부까지 전부 롤백
    @Transactional
    public void requiredInnerFailNotCaught() {
        dao.addBalance("A", -100);
        inner.addAndFail(); // 예외를 잡지 않고 그대로 위로 -> 전체 롤백
    }

    // (3) ★ 함정: 내부(REQUIRED) 실패를 외부가 try-catch로 '삼키고' 커밋 시도
    //     -> 내부가 트랜잭션을 rollbackOnly로 표시해 둠 -> 외부 커밋 시 UnexpectedRollbackException
    @Transactional
    public void requiredInnerFailCaught() {
        dao.addBalance("A", -100);
        try {
            inner.addAndFail();
        } catch (RuntimeException e) {
            System.out.println("    [outer] 내부 예외를 catch로 삼킴(계속 진행하려 함): " + e.getMessage());
        }
        System.out.println("    [outer] 정상 종료처럼 보이지만... 커밋 시점에 무슨 일이?");
        // 메서드 정상 종료 -> 프록시가 커밋 시도 -> rollbackOnly 발견 -> UnexpectedRollbackException
    }

    // (4) REQUIRES_NEW + 내부 실패를 외부가 삼킴: 내부는 '별도 트랜잭션'이라 내부만 롤백, 외부는 커밋
    @Transactional
    public void requiresNewInnerFailCaught() {
        dao.addBalance("A", -100);
        try {
            inner.addAndFailNew();
        } catch (RuntimeException e) {
            System.out.println("    [outer] 내부(REQUIRES_NEW) 예외를 catch로 삼킴: " + e.getMessage());
        }
        System.out.println("    [outer] 정상 종료 -> 외부는 커밋(A-100만 반영, 내부 B는 별도 롤백)");
    }

    // (5) NESTED + 내부 실패를 외부가 삼킴: 내부는 외부와 '같은 트랜잭션 안의 Savepoint'라, Savepoint까지만
    //     부분 롤백되고 외부는 계속 진행해 커밋된다(REQUIRES_NEW와 결과는 비슷하나 커넥션 공유 + 외부 종속).
    @Transactional
    public void nestedInnerFailCaught() {
        dao.addBalance("A", -100);
        try {
            inner.addAndFailNested();
        } catch (RuntimeException e) {
            System.out.println("    [outer] 내부(NESTED) 예외를 catch로 삼킴: " + e.getMessage());
        }
        System.out.println("    [outer] 정상 종료 -> 외부 커밋(A-100 반영, 내부 B는 Savepoint까지 롤백)");
    }
}
