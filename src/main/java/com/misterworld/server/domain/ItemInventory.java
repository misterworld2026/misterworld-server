package com.misterworld.server.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "물품재고")
@Getter
public class ItemInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "물품재고_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "여행상품_ID", nullable = false)
    @Setter
    private TravelProduct travelProduct;

    @Column(name = "물품명", nullable = false, length = 255)
    @Setter
    private String itemName;

    @Column(name = "재고수량", nullable = false)
    @Setter
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "재고상태", nullable = false, length = 40)
    @Setter
    private InventoryStatus status;

    @Column(name = "수정일시", nullable = false)
    @Setter
    private LocalDateTime updatedAt;

}
