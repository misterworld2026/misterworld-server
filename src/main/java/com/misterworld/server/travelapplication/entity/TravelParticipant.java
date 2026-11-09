package com.misterworld.server.travelapplication.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "여행객")
@Getter
public class TravelParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행객_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행신청_ID", nullable = false)
    @Setter
    private TravelApplication travelApplication;

    @Column(name = "이름", nullable = false, length = 255)
    @Setter
    private String name;

    @Column(name = "연락처", nullable = false, length = 255)
    @Setter
    private String phoneNumber;

    @Column(name = "생년월일", nullable = false)
    @Setter
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "참가자구분", nullable = false, length = 40)
    @Setter
    private ParticipantType participantType;

}
