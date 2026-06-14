package com.study.part10_db.domain;

/**
 * [공유 도메인] PART 10(DB 접근의 진화) 전체에서 쓰는 단순 도메인.
 *
 * id/name/age만 가진다. PART 10의 학습 주제는 'Customer를 어떻게 설계하느냐'가 아니라 'DB 접근 코드를
 * 어떻게 추상화·진화시키느냐(JDBC -> JdbcTemplate)'이므로 도메인은 최소로 둔다.
 * (10.5에서 BeanPropertyRowMapper가 컬럼명 name/age <-> 필드명 name/age를 자동 매핑하는 걸 보여줄 때 쓰인다.)
 */
public class Customer {
    private String id;
    private String name;
    private int age;

    public Customer() {}

    public Customer(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name=" + name + ", age=" + age + "}";
    }
}
