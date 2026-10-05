package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "회원")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    public enum Role {
        MEMBER,
        STAFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "회원_ID")
    private Long id;

    @Column(name = "이메일", nullable = false)
    private String email;

    @Column(name = "비밀번호", nullable = false)
    private String password;

    @Column(name = "이름", nullable = false)
    private String name;

    @Column(name = "주소")
    private String address;

    @Column(name = "연락처")
    private String phoneNumber;

    @Column(name = "여행신청인원수")
    private Integer TravelCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "회원구분")
    private Role role;
}
