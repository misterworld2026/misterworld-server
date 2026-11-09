package com.misterworld.server.travelapplication.entity;

import com.misterworld.server.member.entity.Member;
import com.misterworld.server.domain.TravelSchedule;


import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "여행신청")
@Getter
public class TravelApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행신청_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "회원_ID", nullable = false)
    @Setter
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행일정_ID", nullable = false)
    @Setter
    private TravelSchedule travelSchedule;

    @Column(name = "투어등급", nullable = false, length = 255)
    @Setter
    private String tourGrade;

    @Column(name = "호텔옵션", nullable = false, length = 255)
    @Setter
    private String hotelOption;

    @Column(name = "교통옵션", nullable = false, length = 255)
    @Setter
    private String transportOption;

    @Column(name = "식사옵션", nullable = false, length = 255)
    @Setter
    private String mealOption;

    @Column(name = "추가옵션", nullable = true, length = 2000)
    @Setter
    private String extraOption;

    @Column(name = "신청인원", nullable = false)
    @Setter
    private Integer applicantCount;

    @Column(name = "기본가격", nullable = false)
    @Setter
    private Integer basePrice;

    @Column(name = "옵션추가금액", nullable = false)
    @Setter
    private Integer additionalOptionPrice;

    @Column(name = "할인율", nullable = false)
    @Setter
    private Integer discountRate;

    @Column(name = "할인금액", nullable = false)
    @Setter
    private Integer discountAmount;

    @Column(name = "최종가격", nullable = false)
    @Setter
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "신청상태", nullable = false, length = 40)
    @Setter
    private ApplicationStatus status;

    @Column(name = "신청일시", nullable = false)
    @Setter
    private LocalDateTime appliedAt;

    @Column(name="요청키", length=64) @Setter private String requestKey;
    @Column(name="요청해시", length=64) @Setter private String requestHash;
    @Column(name="취소일시") @Setter private LocalDateTime cancelledAt;
    @Enumerated(EnumType.STRING)
    @Column(name="결제상태", nullable=false, length=20)
    @Setter private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    @Column(name="결제금액") @Setter private Integer paidAmount;
    @Column(name="결제완료일시") @Setter private LocalDateTime paidAt;
}
