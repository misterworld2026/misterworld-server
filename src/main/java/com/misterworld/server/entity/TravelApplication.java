package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "여행신청")
@Getter
@Setter
@NoArgsConstructor
public class TravelApplication {

    public enum Status {
        PENDING, // 신청 대기
        CONFIRMED, // 신청 확정
        CANCELLED // 신청 취소
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행신청_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "여행일정_ID", nullable = false)
    private TravelSchedule travelSchedule;

    @Column(name = "투어등급")
    private String tourGrade;

    @Column(name = "호텔옵션")
    private String hotelOption;

    @Column(name = "식사옵션")
    private String mealOption;

    @Column(name = "추가옵션")
    private String extraOption;

    @Column(name = "신청인원")
    private Integer applicantCount;

    @Column(name = "기본가격")
    private Integer basePrice;

    @Column(name = "옵션추가금액")
    private Integer additionalOptionPrice;

    @Column(name = "할인율")
    private Integer discountRate;

    @Column(name = "최종가격")
    private Integer finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "신청상태")
    private Status status = Status.PENDING;

    @Column(name = "신청일시")
    private LocalDateTime appliedAt;

    @PrePersist
    private void setAppliedAt() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

}
