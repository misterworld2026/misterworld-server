package com.misterworld.server.controller;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.*;
import com.misterworld.server.service.ItemInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class ItemInventoryController {
    private final ItemInventoryService service;

    @GetMapping
    public Page<InventoryResponse> getInventoryList(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.getInventoryList(Pages.of(page, size, "id"));
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public InventoryResponse registerInventory(@Valid @RequestBody ItemInventoryRequest request) {
        return service.registerInventory(request);
    }

    public record QuantityRequest(
        @NotNull @Min(0) Integer quantity
    ) {
    }

    @PatchMapping("/{id}")
    public InventoryResponse updateInventory(@PathVariable("id") Long inventoryId,
                                             @Valid @RequestBody QuantityRequest request) {
        return service.updateInventory(inventoryId, request.quantity());
    }

    public Page<InventoryResponse> getInventory(int page, int size) {
        return getInventoryList(page, size);
    }

    public InventoryResponse registerItem(ItemInventoryRequest request) {
        return registerInventory(request);
    }

    public InventoryResponse updateQuantity(Long itemId, Integer quantity) {
        return service.updateQuantity(itemId, quantity);
    }
}
