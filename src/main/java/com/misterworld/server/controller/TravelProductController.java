package com.misterworld.server.controller;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.*;
import com.misterworld.server.service.TravelProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TravelProductController {
    private final TravelProductService service;

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
