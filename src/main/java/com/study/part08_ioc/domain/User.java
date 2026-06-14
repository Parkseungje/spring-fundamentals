package com.study.part08_ioc.domain;

/**
 * [공유 도메인] PART 8 리팩토링 여정 전체에서 쓰는 단순 도메인 객체.
 *
 * 이 클래스는 stage1~stage5 모든 단계가 공유한다. PART 8의 학습 주제는 'User를 어떻게 저장하느냐'가
 * 아니라 'UserDao(저장 책임 객체)를 어떻게 설계·진화시키느냐'이므로, 도메인 자체는 id/name만 가진
 * 가장 단순한 형태로 둔다.
 */
public class User {
    private String id;
    private String name;

    public User() {}

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name=" + name + "}";
    }
}
