package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "여행일정")
@Getter
@Setter
@NoArgsConstructor
public class TravelSchedule {

    public enum Status {
        RECRUITING, // 모집중
        CLOSED, // 모집마강
        IN_PROGRESS, // 여행 진행중
        COMPLETED, // 여행 완료
        CANCELLED // 일정 취소
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행일정_ID")
    private Long travelScheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    private TravelProduct travelProduct;

    @Column(name = "출발일")
    private LocalDate departureDate;

    @Column(name = "종료일")
    private LocalDate arrivalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "여행상태")
    private Status status;

    @Column(name = "현재신청인원")
    private Integer applicantCount;

}
