package com.misterworld.server.controller;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.*;
import com.misterworld.server.service.TravelScheduleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

@RestController
@RequiredArgsConstructor
public class TravelScheduleController {
    private final TravelScheduleService service;

    @GetMapping("/api/tours/schedules")
    public Page<TravelScheduleResponse> getSchedules(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.getSchedules(null, Pages.of(page, size, "departureDate"));
    }

    @GetMapping("/api/tours/{tourId}/schedules")
    public Page<TravelScheduleResponse> getSchedules(@PathVariable("tourId") Long productId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.getSchedules(productId, Pages.of(page, size, "departureDate"));
    }

    @PostMapping("/api/admin/tours/{tourId}/schedules")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public TravelScheduleResponse createSchedule(@PathVariable("tourId") Long productId,
                                                 @Valid @RequestBody TravelScheduleRequest request) {
        return service.saveSchedule(productId, request);
    }

    @PatchMapping("/api/admin/schedules/{scheduleId}")
    public TravelScheduleResponse updateSchedule(@PathVariable Long scheduleId,
                                                 @Valid @RequestBody TravelScheduleRequest request) {
        return service.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/api/admin/schedules/{scheduleId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Long scheduleId) {
        service.deleteSchedule(scheduleId);
    }
}
