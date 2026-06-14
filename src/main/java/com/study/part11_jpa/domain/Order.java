package com.study.part11_jpa.domain;

/**
 * [공유 도메인] 주문 객체. 핵심: 주문은 회원을 'member_id(숫자)'가 아니라 '참조(Member 객체)'로 들고 있다.
 *
 * 이것이 객체 세계의 방식이다 — order.getMember().getName()처럼 객체 그래프를 따라간다. 반면 DB(관계형)는
 * orders 테이블이 member_id(외래 키, 숫자)만 갖는다. 이 '참조 vs 외래 키'의 차이가 11.2의 미스매치 중 하나다.
 */
public class Order {
    private Long id;
    private String item;
    private Member member;   // 외래 키(숫자)가 아니라 '객체 참조'

    public Order() {}

    public Order(Long id, String item, Member member) {
        this.id = id;
        this.item = item;
        this.member = member;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", item=" + item + ", member=" + member + "}";
    }
}
