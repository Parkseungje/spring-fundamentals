package com.study.part11_jpa.s05_transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [11.5] @Transactional — 선언적 트랜잭션. 어노테이션 한 줄로 트랜잭션을 건다.
 *
 * 진화의 끝: 10.4에서는 conn.setAutoCommit(false)/commit()/rollback()/close()를 손으로 썼다. @Transactional은
 * 그 보일러플레이트를 '프록시(AOP)'가 대신 끼워 넣어 없앤다. 메서드 시작 시 트랜잭션 시작, 정상 종료 시
 * 커밋, 런타임 예외 발생 시 롤백을 자동으로 처리한다.
 *
 * ★ 동작 원리(프록시): @Transactional이 붙은 빈은 Spring이 '대리자(프록시) 객체'로 감싸 등록한다. 외부에서
 *   transfer()를 호출하면 프록시가 먼저 '트랜잭션 시작'을 하고 -> 진짜 메서드를 호출 -> 끝나면 '커밋/롤백'을
 *   한다. (이 프록시의 정체가 PART 12 AOP다.)
 *
 * ★ 5가지 함정(아래 메서드/주석으로 일부 시연, 나머지는 docs):
 *   1) private 메서드엔 안 걸림 — 프록시가 가로채지 못함(프록시는 외부 호출만 가로챔).
 *   2) self-invocation — 같은 클래스 안에서 this.다른메서드() 호출은 프록시를 안 거쳐 트랜잭션이 안 걸림.
 *   3) 기본 롤백은 Runtime(unchecked) 예외만 — 체크 예외는 rollbackFor 지정 필요.
 *   4) 전파(propagation): REQUIRED(기본, 있으면 참여), REQUIRES_NEW(항상 새), NESTED(중첩/Savepoint).
 *   5) readOnly=true: 읽기 전용 최적화(영속성 컨텍스트 플러시 생략 등)로 조회 성능 ↑.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {   // 생성자 주입(PART 8.6)
        this.accountRepository = accountRepository;
    }

    // 정상 이체: 출금 + 입금이 한 트랜잭션. 끝나면 자동 커밋(변경 감지로 UPDATE 반영).
    @Transactional
    public void transfer(Long fromId, Long toId, int amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();
        from.withdraw(amount);   // 변경 감지(dirty checking) — save() 호출 불필요
        to.deposit(amount);
    }

    // 도중 예외 -> @Transactional이 자동 롤백 -> 출금도 없던 일로(원자성). (10.4의 수동 rollback을 어노테이션이 대신)
    @Transactional
    public void transferWithError(Long fromId, Long toId, int amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        from.withdraw(amount);                       // 잔액 변경(아직 커밋 전)
        if (true) throw new RuntimeException("이체 중 장애!");   // RuntimeException -> 자동 롤백
    }

    // 읽기 전용: 조회만 하는 메서드는 readOnly=true로 최적화(함정 5).
    @Transactional(readOnly = true)
    public int balanceOf(Long id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }
}
