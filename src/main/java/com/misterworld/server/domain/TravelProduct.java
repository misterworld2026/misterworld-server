package com.misterworld.server.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "여행상품")
@Getter
public class TravelProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행상품_ID", nullable = false)
    private Long id;

    @Column(name = "상품명", nullable = false, length = 255)
    @Setter
    private String name;

    @Column(name = "테마", nullable = false, length = 255)
    @Setter
    private String theme;

    @Column(name = "상품설명", nullable = true, length = 2000)
    @Setter
    private String description;

    @Column(name = "클래식_가격", nullable = true)
    @Setter
    private Integer classicPrice;

    @Column(name = "그랜드_가격", nullable = false)
    @Setter
    private Integer grandPrice;

    @Column(name = "프리미엄_가격", nullable = false)
    @Setter
    private Integer premiumPrice;

}
