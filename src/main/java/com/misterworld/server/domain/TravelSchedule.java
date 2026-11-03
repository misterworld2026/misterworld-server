package com.misterworld.server.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "여행일정")
@Getter
public class TravelSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행일정_ID", nullable = false)
    private Long travelScheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    @Setter
    private TravelProduct travelProduct;

    @Column(name = "출발일", nullable = false)
    @Setter
    private LocalDate departureDate;

    @Column(name = "종료일", nullable = false)
    @Setter
    private LocalDate arrivalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "여행상태", nullable = false, length = 40)
    @Setter
    private ScheduleStatus status;

    @Column(name = "현재신청인원", nullable = false)
    @Setter
    private Integer applicantCount;

}
