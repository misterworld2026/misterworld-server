package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "여행참가자")
@Getter
@Setter
@NoArgsConstructor
public class TravelParticipant {

    public enum ParticipantType {
        APPLICANT, // 신청자
        PARTICIPANT // 참가자
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행참가자_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "여행신청_ID", nullable = false)
    private TravelApplication travelApplication;

    @Column(name = "이름")
    private String name;

    @Column(name = "연락처")
    private String phoneNumber;

    @Column(name = "생년월일")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "참가자구분")
    private ParticipantType participantType;

}
