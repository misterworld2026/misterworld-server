package com.misterworld.server.travelproduct.controller;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.common.Pages;
import com.misterworld.server.travelproduct.dto.TravelOptionResponse;
import com.misterworld.server.travelproduct.dto.TravelProductRequest;
import com.misterworld.server.travelproduct.dto.TravelProductResponse;
import com.misterworld.server.travelproduct.dto.TravelSelectionRequest;
import com.misterworld.server.travelproduct.dto.TravelSelectionResponse;
import com.misterworld.server.travelproduct.service.TravelProductService;
import com.misterworld.server.travelschedule.service.TravelScheduleService;


import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TravelProductController {
    private final TravelProductService service;
    private final TravelScheduleService schedules;

    @PostMapping("/api/tours/{tourId}/selection/validate")
    public TravelSelectionResponse validateSelection(@PathVariable("tourId") Long productId,
            @Valid @RequestBody TravelSelectionRequest request) {
        var selected = service.validateSelection(productId, request.grade(), request.optionIds());
        var schedule = schedules.validateSelection(productId, request.scheduleId());
        var product = service.getProduct(productId);
        var configuration = product.allowedGrades().stream().filter(g -> g.grade().equals(request.grade()))
            .findFirst().orElseThrow(BusinessException::policy);
        var normalized = selected.stream().map(TravelOptionResponse::from)
            .sorted(java.util.Comparator.comparing(TravelOptionResponse::type).thenComparing(TravelOptionResponse::id))
            .toList();
        var defaultIds = configuration.defaultOptions().stream().map(TravelOptionResponse::id).toList();
        return new TravelSelectionResponse(product, schedule, request.grade(), configuration.defaultOptions(),
            normalized, normalized.stream().filter(o -> !defaultIds.contains(o.id())).toList());
    }


    @GetMapping("/api/tours")
    public Page<TravelProductResponse> getProducts(@RequestParam(required = false) String theme,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.getProducts(theme, Pages.of(page, size, "id"));
    }

    @GetMapping("/api/tours/{tourId}")
    public TravelProductResponse getProduct(@PathVariable("tourId") Long productId) {
        return service.getProduct(productId);
    }

    @GetMapping("/api/tours/{tourId}/options")
    public List<TravelOptionResponse> getOptions(@PathVariable("tourId") Long productId) {
        return service.getOptions(productId);
    }

    @PostMapping("/api/admin/tours")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public TravelProductResponse createProduct(@Valid @RequestBody TravelProductRequest request) {
        return saveProduct(request);
    }

    public TravelProductResponse saveProduct(TravelProductRequest request) {
        return service.saveProduct(request);
    }

    @PutMapping("/api/admin/tours/{tourId}")
    public TravelProductResponse updateProduct(@PathVariable("tourId") Long productId,
                                               @Valid @RequestBody TravelProductRequest request) {
        return service.saveProduct(productId, request);
    }
}
