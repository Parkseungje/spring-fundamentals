package com.study.part11_jpa.s05_transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [11.5] @Transactional이 자동으로 커밋/롤백하는지 검증한다.
 *
 * @SpringBootTest: 전체 컨텍스트를 띄운다(AccountService가 '프록시 빈'으로 등록되어야 @Transactional이
 * 동작하므로 슬라이스가 아닌 풀 컨텍스트 사용). 테스트 자체엔 @Transactional을 붙이지 않는다 — 그래야
 * 서비스의 commit/rollback이 '진짜로' 일어나고, 그 결과를 다시 읽어 확인할 수 있다.
 */
@SpringBootTest
class TransactionalTest {

    @Autowired AccountService accountService;
    @Autowired AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        // 매 테스트 시작 상태: A=1000, B=1000 (9.1 픽스처 — 테스트 독립성)
        accountRepository.deleteAll();
        accountRepository.save(new Account(1L, 1000));
        accountRepository.save(new Account(2L, 1000));
    }

    @Test
    @DisplayName("정상 이체 — 출금+입금이 함께 커밋된다")
    void transferCommits() {
        accountService.transfer(1L, 2L, 300);

        assertThat(accountService.balanceOf(1L)).isEqualTo(700);    // 출금 반영
        assertThat(accountService.balanceOf(2L)).isEqualTo(1300);   // 입금 반영
    }

    @Test
    @DisplayName("이체 중 예외 — @Transactional이 자동 롤백해 출금도 취소된다(원자성)")
    void transferRollsBackOnError() {
        assertThatThrownBy(() -> accountService.transferWithError(1L, 2L, 300))
                .isInstanceOf(RuntimeException.class);

        // 출금(withdraw)을 했지만 예외로 롤백 -> 잔액은 원래대로. 수동 rollback 없이 어노테이션이 처리.
        assertThat(accountService.balanceOf(1L)).isEqualTo(1000);   // 변경 안 됨 = 롤백됨
        assertThat(accountService.balanceOf(2L)).isEqualTo(1000);
    }
}
