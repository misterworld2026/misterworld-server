package com.misterworld.server.inventory.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.inventory.dto.InventoryResponse;
import com.misterworld.server.inventory.dto.ItemInventoryRequest;
import com.misterworld.server.inventory.entity.InventoryStatus;
import com.misterworld.server.inventory.entity.ItemInventory;
import com.misterworld.server.inventory.repository.ItemInventoryRepository;
import com.misterworld.server.travelproduct.repository.TravelProductRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

import java.time.*;

import static com.misterworld.server.common.BusinessException.require;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemInventoryService {
    private final ItemInventoryRepository inventory;
    private final TravelProductRepository products;
    private final Clock clock;

    public Page<InventoryResponse> getInventoryList(Pageable pageable) {
        return inventory.findAll(pageable).map(InventoryResponse::from);
    }

    public Page<InventoryResponse> getInventory(Pageable pageable) {
        return getInventoryList(pageable);
    }

    @Transactional
    public InventoryResponse registerInventory(ItemInventoryRequest request) {
        validateQuantity(request.quantity());
        var itemInventory = new ItemInventory();
        itemInventory.setTravelProduct(products.findById(request.productId()).orElseThrow(BusinessException::notFound));
        itemInventory.setItemName(request.itemName());
        itemInventory.setQuantity(request.quantity());
        updateStockStatus(itemInventory);
        itemInventory.setUpdatedAt(LocalDateTime.now(clock));
        var savedInventory = inventory.save(itemInventory);
        return InventoryResponse.from(savedInventory);
    }

    @Transactional
    public InventoryResponse registerItem(ItemInventoryRequest request) {
        return registerInventory(request);
    }

    @Transactional
    public InventoryResponse updateInventory(Long inventoryId, Integer quantity) {
        validateQuantity(quantity);
        var itemInventory = inventory.findByIdForUpdate(inventoryId).orElseThrow(BusinessException::notFound);
        itemInventory.setQuantity(quantity);
        updateStockStatus(itemInventory);
        itemInventory.setUpdatedAt(LocalDateTime.now(clock));
        return InventoryResponse.from(itemInventory);
    }

    @Transactional
    public InventoryResponse updateQuantity(Long itemId, Integer quantity) {
        return updateInventory(itemId, quantity);
    }

    public void validateQuantity(Integer quantity) {
        require(quantity != null && quantity >= 0, "재고 수량은 0 이상의 정수여야 합니다.");
    }

    public InventoryStatus determineInventoryStatus(Integer quantity) {
        validateQuantity(quantity);
        return quantity == 0 ? InventoryStatus.OUT_OF_STOCK : quantity <= 3 ? InventoryStatus.LOW_STOCK :
            InventoryStatus.NORMAL;
    }

    public void updateStockStatus(ItemInventory itemInventory) {
        itemInventory.setStatus(determineInventoryStatus(itemInventory.getQuantity()));
    }
}
