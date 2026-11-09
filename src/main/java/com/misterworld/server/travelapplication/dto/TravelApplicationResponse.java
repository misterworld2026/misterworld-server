package com.misterworld.server.travelapplication.dto;

import com.misterworld.server.travelschedule.entity.ScheduleStatus;
/** PDF p.24 booking result; existing ApplicationResponse remains for existing queries. */
public record TravelApplicationResponse(ApplicationResponse application, ScheduleStatus scheduleStatus,
    boolean departureConfirmed) {}
