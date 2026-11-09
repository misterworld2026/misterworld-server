package com.misterworld.server.notification.entity;

import com.misterworld.server.travelapplication.entity.TravelApplication;


import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "문자알림")
@Getter
public class SmsNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "문자알림_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행신청_ID", nullable = false)
    @Setter
    private TravelApplication travelApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "알림종류", nullable = false, length = 40)
    @Setter
    private NotificationType notificationType;

    @Column(name = "수신번호", nullable = false, length = 255)
    @Setter
    private String recipientNumber;

    @Column(name = "메시지내용", nullable = false, length = 2000)
    @Setter
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "발송상태", nullable = false, length = 40)
    @Setter
    private NotificationStatus status;

    @Column(name = "발송일시", nullable = true)
    @Setter
    private LocalDateTime sentAt;

    @Column(name="생성일시") @Setter private LocalDateTime createdAt;
    @Column(name="다음시도일시") @Setter private LocalDateTime nextAttemptAt;
    @Column(name="잠금만료일시") @Setter private LocalDateTime lockedUntil;
    @Column(name="처리토큰", length=36) @Setter private String claimToken;
    @Column(name="시도횟수", nullable=false) @Setter private Integer attemptCount=0;
    @Column(name="마지막오류", length=40) @Setter private String lastError;
}
