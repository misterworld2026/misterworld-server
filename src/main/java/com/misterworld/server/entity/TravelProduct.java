package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "여행상품")
@Getter
@Setter
@NoArgsConstructor
public class TravelProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행상품_ID")
    private Long id;

    @Column(name = "상품명")
    private String name;

    @Column(name = "테마")
    private String theme;

    @Column(name = "상품설명")
    private String description;

    @Column(name = "클래식_가격")
    private Integer classPrice;

    @Column(name = "그랜드_가격")
    private Integer grandPrice;

    @Column(name = "프리미엄_가격")
    private Integer premiumPrice;

}
