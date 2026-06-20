package com.study.part13_tx.s06_isolation;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [13.6] '지금 트랜잭션의 격리 수준'을 읽어 사람이 읽을 이름으로 바꿔 주는 헬퍼.
 *
 * TransactionSynchronizationManager.getCurrentTransactionIsolationLevel():
 *   현재 트랜잭션에 '설정된' 격리 수준을 돌려준다. 스프링이 @Transactional(isolation=...)을 적용할 때 이
 *   값을 바인딩한다. null이면 "따로 지정 안 함 = DB 기본 격리 수준 사용".
 *   숫자는 java.sql.Connection의 상수와 같다: 1=READ_UNCOMMITTED, 2=READ_COMMITTED, 4=REPEATABLE_READ, 8=SERIALIZABLE.
 */
public final class IsoUtil {

    private IsoUtil() {
    }

    public static String current() {
        Integer level = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        return name(level);
    }

    static String name(Integer level) {
        if (level == null) return "DEFAULT(지정 안 함 -> DB 기본)";
        return switch (level) {
            case 1 -> "READ_UNCOMMITTED(1)";
            case 2 -> "READ_COMMITTED(2)";
            case 4 -> "REPEATABLE_READ(4)";
            case 8 -> "SERIALIZABLE(8)";
            default -> "UNKNOWN(" + level + ")";
        };
    }
}
