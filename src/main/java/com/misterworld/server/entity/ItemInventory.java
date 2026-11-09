package com.misterworld.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "물품재고")
@Getter
@Setter
@NoArgsConstructor
public class ItemInventory {

    public enum Status {
        NORMAL, // 정상
        LOW_STOCK, // 부족 상태
        OUT_OF_STOCK // 품절 상태
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "물품재고_ID")
    private Long id;

    // 어떤 테마의 여행인지에 따라 기념품이 달라짐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    private TravelProduct travelProduct;

    @Column(name = "물품명", nullable = false)
    private String itemName;

    @Column(name = "재고수량", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "재고상태")
    private Status status;

    @Column(name = "수정일시")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
