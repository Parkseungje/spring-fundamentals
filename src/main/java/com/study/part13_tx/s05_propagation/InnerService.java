package com.study.part13_tx.s05_propagation;

import com.study.part13_tx.s02_declarative_vs_programmatic.AccountDao;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * [13.5] 내부 트랜잭션 — 전파(propagation) 옵션에 따라 외부와 '한 트랜잭션으로 묶이느냐'가 달라진다.
 *
 * 전파(propagation): "이미 진행 중인 트랜잭션이 있을 때, 내가 호출되면 어떻게 할까?"의 규칙이다.
 *  - REQUIRED(기본): 진행 중 트랜잭션이 있으면 '참여'(같은 물리 트랜잭션), 없으면 새로 시작.
 *  - REQUIRES_NEW: 항상 '새 물리 트랜잭션'을 시작. 외부와 완전히 분리(내부 롤백이 외부에 영향 X).
 *
 * 이 빈은 OuterService가 주입받아 호출한다(별도 빈 = 프록시 경유, 13.3). 그래야 @Transactional이 적용된다.
 */
public class InnerService {

    private final AccountDao dao;

    public InnerService(AccountDao dao) {
        this.dao = dao;
    }

    // REQUIRED + 정상: 외부 트랜잭션에 참여해 B에 +100 (외부와 함께 커밋된다)
    @Transactional(propagation = Propagation.REQUIRED)
    public void addOk() {
        dao.addBalance("B", 100);
        System.out.println("    [inner REQUIRED] B에 +100 (외부 트랜잭션에 참여)");
    }

    // REQUIRED + 실패: 같은 물리 트랜잭션에 참여한 상태로 예외 -> 트랜잭션을 rollbackOnly로 표시
    @Transactional(propagation = Propagation.REQUIRED)
    public void addAndFail() {
        dao.addBalance("B", 100);
        System.out.println("    [inner REQUIRED] B에 +100 후 예외 발생 -> 트랜잭션 rollbackOnly 표시됨");
        throw new RuntimeException("inner 실패(REQUIRED)");
    }

    // REQUIRES_NEW + 실패: 외부와 '분리된' 새 물리 트랜잭션. 이 실패는 이 트랜잭션만 롤백(외부엔 영향 X)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addAndFailNew() {
        dao.addBalance("B", 100);
        System.out.println("    [inner REQUIRES_NEW] B에 +100 후 예외 발생 -> '이 새 트랜잭션만' 롤백");
        throw new RuntimeException("inner 실패(REQUIRES_NEW)");
    }

    // NESTED + 실패: 외부와 '같은 물리 트랜잭션' 안의 Savepoint. 외부가 이 예외를 잡으면 Savepoint까지만
    // 부분 롤백(B+100 취소)되고 외부는 계속 진행할 수 있다. 단 REQUIRES_NEW와 달리 커넥션은 외부와 공유하며,
    // 외부가 나중에 롤백되면 이 NESTED 작업도 함께 사라진다(외부에 종속). DataSourceTransactionManager 필요.
    @Transactional(propagation = Propagation.NESTED)
    public void addAndFailNested() {
        dao.addBalance("B", 100);
        System.out.println("    [inner NESTED] B에 +100 후 예외 발생 -> Savepoint까지만 부분 롤백");
        throw new RuntimeException("inner 실패(NESTED)");
    }
}
