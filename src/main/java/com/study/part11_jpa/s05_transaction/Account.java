package com.study.part11_jpa.s05_transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * [11.5] 계좌 엔티티 — 트랜잭션(이체) 데모용.
 *
 * withdraw/deposit는 balance를 바꾸는 행동이다. JPA의 '변경 감지(dirty checking)' 덕분에, 트랜잭션 안에서
 * 조회한 엔티티의 필드를 바꾸면 별도 save() 없이도 커밋 시점에 UPDATE가 자동 반영된다.
 * (단, 트랜잭션이 롤백되면 이 변경은 DB에 반영되지 않는다 — 그게 11.5의 핵심.)
 */
@Entity
public class Account {

    @Id
    private Long id;     // 데모 단순화를 위해 PK를 직접 지정(자동 생성 X)
    private int balance;

    protected Account() {}

    public Account(Long id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public void withdraw(int amount) { this.balance -= amount; }
    public void deposit(int amount)  { this.balance += amount; }

    public Long getId() { return id; }
    public int getBalance() { return balance; }
}
