package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "문자알림")
@Getter
@Setter
@NoArgsConstructor
public class SmsNotification {

    public enum Status {
        PENDING, // 발송 대기
        SENT, // 발송 완료
        FAILED // 발송 실패
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "문자알림_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행신청_ID")
    private TravelApplication travelApplication;

    @Column(name = "수신번호")
    private String recipientNumber; // 수신번호

    @Column(name = "메시지내용")
    private String message; // 메시지 내용

    @Enumerated(EnumType.STRING)
    @Column(name = "발송상태")
    private Status status = Status.PENDING;

    @Column(name = "발송일시")
    private LocalDateTime sentAt;


}
