package com.misterworld.server.dto;

import com.misterworld.server.domain.*;

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
