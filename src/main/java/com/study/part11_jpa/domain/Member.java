package com.study.part11_jpa.domain;

/**
 * [공유 도메인] PART 11에서 쓰는 회원 객체. (11.2 객체-관계 미스매치, 11.3~ JPA 매핑에서 사용)
 *
 * 지금은 순수 자바 객체(POJO)다. 11.4에서 여기에 @Entity/@Id 같은 JPA 매핑 어노테이션을 붙여 'DB 테이블과
 * 연결된 객체'로 진화시킨다.
 */
public class Member {
    private Long id;
    private String name;

    public Member() {}

    public Member(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Member{id=" + id + ", name=" + name + "}";
    }
}
