package com.misterworld.server.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "재고차감내역")
@Getter
@Setter
public class InventoryDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "재고차감내역_ID", nullable = false)
    private Long id;

    @Column(name = "여행일정_ID", nullable = false)
    private Long scheduleId;

    @Column(name = "물품재고_ID", nullable = false)
    private Long inventoryId;

    @Column(name = "차감수량", nullable = false)
    private Integer quantity;

    @Column(name = "복구일시")
    private LocalDateTime restoredAt;
}
