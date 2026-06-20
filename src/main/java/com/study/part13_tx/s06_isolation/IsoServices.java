package com.study.part13_tx.s06_isolation;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * [13.6] 격리 수준을 '선언적으로'(@Transactional) 지정하는 서비스들.
 *
 * 10.4에서는 격리 수준을 JDBC의 conn.setTransactionIsolation()(저수준)으로 직접 바꿔 이상현상을 재현했다.
 * 여기서는 Spring 방식 — @Transactional(isolation = Isolation.XXX) 로 선언적으로 지정한다(실무 표준).
 * 그리고 격리 수준이 '전파'와 어떻게 얽히는지(13.5)를 보여주는 inner/outer도 둔다.
 */
public class IsoServices {

    // ── (1) 선언적 격리 지정: 지정 안 함(DEFAULT) vs 명시 ──
    public static class SimpleIso {
        @Transactional // isolation 지정 안 함 -> DB 기본 격리 사용
        public void useDefault() {
            System.out.println("    useDefault()      현재 격리 = " + IsoUtil.current());
        }

        @Transactional(isolation = Isolation.READ_COMMITTED)
        public void readCommitted() {
            System.out.println("    readCommitted()   현재 격리 = " + IsoUtil.current());
        }

        @Transactional(isolation = Isolation.REPEATABLE_READ)
        public void repeatableRead() {
            System.out.println("    repeatableRead()  현재 격리 = " + IsoUtil.current());
        }
    }

    // ── (2)(3) 격리 x 전파 상호작용 ──
    public static class InnerIso {
        // REQUIRED + 다른 격리(SERIALIZABLE)를 '선언'했지만, 외부에 '참여'하면 외부 격리를 따른다(선언 무시).
        @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
        public void participateRequired() {
            System.out.println("    inner(REQUIRED, 선언=SERIALIZABLE) 실제 격리 = " + IsoUtil.current()
                    + "   <- 외부에 '참여'하면 선언한 SERIALIZABLE이 무시되고 외부 격리를 따른다");
        }

        // REQUIRES_NEW + SERIALIZABLE: 새 트랜잭션을 시작하므로 '자기 격리'를 가진다.
        @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
        public void newRequiresNew() {
            System.out.println("    inner(REQUIRES_NEW, 선언=SERIALIZABLE) 실제 격리 = " + IsoUtil.current()
                    + "   <- 새 트랜잭션이라 선언한 SERIALIZABLE이 그대로 적용된다");
        }
    }

    public static class OuterIso {
        private final InnerIso inner;
        public OuterIso(InnerIso inner) { this.inner = inner; }

        @Transactional(isolation = Isolation.READ_COMMITTED)
        public void callParticipating() {
            System.out.println("    outer 격리 = " + IsoUtil.current() + " (READ_COMMITTED로 시작)");
            inner.participateRequired(); // 같은 트랜잭션에 참여 -> 외부 격리 따름
        }

        @Transactional(isolation = Isolation.READ_COMMITTED)
        public void callNew() {
            System.out.println("    outer 격리 = " + IsoUtil.current() + " (READ_COMMITTED로 시작)");
            inner.newRequiresNew(); // 새 트랜잭션 -> 자기 격리(SERIALIZABLE)
        }
    }

    // ── (4) readOnly도 격리와 '같은 규칙' — 트랜잭션 시작 시점 속성이라 참여 시 외부 따름 ──
    public static class ReadOnlyInner {
        // 내부는 readOnly=true를 '선언'한다. 하지만 참여(REQUIRED)면 외부의 readOnly(false)를 따른다.
        @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
        public void participateRequired() {
            boolean ro = org.springframework.transaction.support.TransactionSynchronizationManager
                    .isCurrentTransactionReadOnly();
            System.out.println("    inner(REQUIRED, 선언=readOnly true) 실제 readOnly = " + ro
                    + "   <- 외부(쓰기)에 참여하면 선언한 readOnly가 무시되고 외부를 따른다");
        }

        // REQUIRES_NEW면 새 트랜잭션이라 자기 readOnly가 적용된다.
        @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
        public void newRequiresNew() {
            boolean ro = org.springframework.transaction.support.TransactionSynchronizationManager
                    .isCurrentTransactionReadOnly();
            System.out.println("    inner(REQUIRES_NEW, 선언=readOnly true) 실제 readOnly = " + ro
                    + "   <- 새 트랜잭션이라 선언한 readOnly가 그대로 적용된다");
        }
    }

    public static class ReadOnlyOuter {
        private final ReadOnlyInner inner;
        public ReadOnlyOuter(ReadOnlyInner inner) { this.inner = inner; }

        @Transactional // 외부는 쓰기 트랜잭션(readOnly=false 기본)
        public void callParticipating() {
            System.out.println("    outer readOnly = " + org.springframework.transaction.support
                    .TransactionSynchronizationManager.isCurrentTransactionReadOnly() + " (쓰기 트랜잭션으로 시작)");
            inner.participateRequired();
        }

        @Transactional
        public void callNew() {
            System.out.println("    outer readOnly = " + org.springframework.transaction.support
                    .TransactionSynchronizationManager.isCurrentTransactionReadOnly() + " (쓰기 트랜잭션으로 시작)");
            inner.newRequiresNew();
        }
    }
}
