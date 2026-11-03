package com.misterworld.server.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "회원")
@Getter
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "회원_ID", nullable = false)
    private Long id;

    @Column(name = "이메일", nullable = false, length = 255)
    @Setter
    private String email;

    @Column(name = "비밀번호", nullable = false, length = 255)
    @Setter
    private String password;

    @Column(name = "이름", nullable = false, length = 255)
    @Setter
    private String name;

    @Column(name = "주소", nullable = false, length = 255)
    @Setter
    private String address;

    @Column(name = "연락처", nullable = false, length = 255)
    @Setter
    private String phoneNumber;

    @Column(name = "Field", nullable = false)
    @Setter
    private Integer travelCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "회원구분", nullable = false, length = 40)
    @Setter
    private Role role;

    public void increaseTravelCount() {
        travelCount = Math.addExact(travelCount, 1);
    }
}
