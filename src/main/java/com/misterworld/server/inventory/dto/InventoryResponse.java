package com.misterworld.server.inventory.dto;

import com.misterworld.server.inventory.entity.InventoryStatus;
import com.misterworld.server.inventory.entity.ItemInventory;


import java.time.LocalDateTime;

public record InventoryResponse(
    Long id,
    Long productId,
    String itemName,
    Integer quantity,
    InventoryStatus status,
    LocalDateTime updatedAt
) {
    public static InventoryResponse from(ItemInventory item) {
        return new InventoryResponse(item.getId(), item.getTravelProduct().getId(), item.getItemName(),
            item.getQuantity(), item.getStatus(), item.getUpdatedAt());
    }
}
