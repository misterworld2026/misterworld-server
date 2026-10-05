package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "여행옵션")
@Getter
@Setter
@NoArgsConstructor
public class TravelOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "여행옵션_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    private TravelProduct travelProduct;

    @Column(name = "옵션종류")
    private String type;

    @Column(name = "옵션명")
    private String name;

    @Column(name = "옵션등급")
    private String grade;

    @Column(name = "추가금액")
    private Integer additionalPrice;

    @Column(name = "옵션설명")
    private String description;

}
