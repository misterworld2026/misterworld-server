package com.misterworld.server.travelproduct.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "여행옵션")
@Getter
public class TravelOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행옵션_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    @Setter
    private TravelProduct travelProduct;

    @Column(name = "옵션종류", nullable = false, length = 255)
    @Setter
    private String type;

    @Column(name = "옵션명", nullable = false, length = 255)
    @Setter
    private String name;

    @Column(name = "옵션등급", nullable = false, length = 255)
    @Setter
    private String grade;

    @Column(name = "추가금액", nullable = false)
    @Setter
    private Integer additionalPrice;

    @Column(name = "옵션설명", nullable = true, length = 2000)
    @Setter
    private String description;

    @Column(name = "기본등급", nullable = true, length = 255)
    @Setter
    private String defaultForGrade;

    // Additional design: explicit product/option policy, comma-separated tour grades.
    @Column(name = "허용투어등급", length = 255)
    @Setter
    private String allowedTourGrades;


    // Additional explicit metadata; never infer bundled champagne from a display name.
    @Column(name="부가혜택코드", length=40) @Setter private String benefitCode;

}
