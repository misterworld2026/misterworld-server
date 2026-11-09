package com.misterworld.server.domain;

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

}
