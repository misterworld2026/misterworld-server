package com.misterworld.server.inventory.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.inventory.dto.InventoryResponse;
import com.misterworld.server.inventory.dto.ItemInventoryRequest;
import com.misterworld.server.inventory.entity.InventoryStatus;
import com.misterworld.server.inventory.entity.ItemInventory;
import com.misterworld.server.inventory.entity.InventoryDeduction;
import com.misterworld.server.inventory.repository.InventoryDeductionRepository;
import com.misterworld.server.inventory.repository.ItemInventoryRepository;
import com.misterworld.server.repository.TravelProductRepository;


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
    private final InventoryDeductionRepository deductions;
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

    @Transactional
    public void deductForConfirmedApplicants(Long scheduleId, Long productId, int applicantCount) {
        require(applicantCount > 0, "확정 인원은 양수여야 합니다.");
        var items = inventory.findByProductIdForUpdate(productId);
        for (var item : items) {
            if (item.getQuantity() < applicantCount)
                throw new BusinessException(409, "INSUFFICIENT_STOCK", item.getItemName() + " 재고가 부족합니다.");
        }
        var byInventoryId = new java.util.HashMap<Long, InventoryDeduction>();
        for (var deduction : deductions.findByScheduleId(scheduleId))
            byInventoryId.put(deduction.getInventoryId(), deduction);
        var now = LocalDateTime.now(clock);
        for (var item : items) {
            var deduction = byInventoryId.get(item.getId());
            if (deduction == null) {
                deduction = new InventoryDeduction();
                deduction.setScheduleId(scheduleId);
                deduction.setInventoryId(item.getId());
                deduction.setQuantity(applicantCount);
                deductions.save(deduction);
            } else {
                if (deduction.getRestoredAt() != null)
                    throw new BusinessException(409, "INVALID_STATE", "복구된 일정의 재고는 다시 차감할 수 없습니다.");
                deduction.setQuantity(Math.addExact(deduction.getQuantity(), applicantCount));
            }
            item.setQuantity(item.getQuantity() - applicantCount);
            updateStockStatus(item);
            item.setUpdatedAt(now);
        }
    }

    @Transactional
    public void restoreForCancelledSchedule(Long scheduleId) {
        var records = deductions.findByScheduleIdAndRestoredAtIsNullOrderByInventoryId(scheduleId);
        var now = LocalDateTime.now(clock);
        for (var record : records) {
            var item = inventory.findByIdForUpdate(record.getInventoryId()).orElseThrow(BusinessException::notFound);
            item.setQuantity(Math.addExact(item.getQuantity(), record.getQuantity()));
            updateStockStatus(item);
            item.setUpdatedAt(now);
            record.setRestoredAt(now);
        }
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
