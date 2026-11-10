package com.misterworld.server.travelschedule.controller;

import com.misterworld.server.common.Pages;
import com.misterworld.server.travelschedule.dto.ScheduleResponse;
import com.misterworld.server.travelschedule.dto.TravelScheduleRequest;
import com.misterworld.server.travelschedule.dto.TravelScheduleResponse;
import com.misterworld.server.travelschedule.service.TravelScheduleService;


import lombok.RequiredArgsConstructor;

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
    public ScheduleResponse createSchedule(@PathVariable("tourId") Long productId,
                                                 @Valid @RequestBody TravelScheduleRequest request) {
        return service.saveSchedule(productId, request);
    }

    @PatchMapping("/api/admin/schedules/{scheduleId}")
    public ScheduleResponse updateSchedule(@PathVariable Long scheduleId,
                                                 @Valid @RequestBody TravelScheduleRequest request) {
        return service.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/api/admin/schedules/{scheduleId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Long scheduleId) {
        service.deleteSchedule(scheduleId);
    }
    @PutMapping("/api/admin/schedules/{scheduleId}/cancel")
    public TravelScheduleResponse cancelSchedule(@PathVariable Long scheduleId) {
        return service.cancelSchedule(scheduleId);
    }

}
