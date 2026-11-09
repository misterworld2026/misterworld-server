package com.misterworld.server.travelschedule.service;

import com.misterworld.server.travelapplication.service.BookingPolicy;
import com.misterworld.server.travelschedule.repository.TravelScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;

import static com.misterworld.server.travelschedule.entity.ScheduleStatus.*;

@Service
@RequiredArgsConstructor
public class ScheduleStatusScheduler {
    private final TravelScheduleRepository schedules;
    private final BookingPolicy policy;
    private final Clock clock;

    @Scheduled(cron = "${app.schedule.transition-cron:0 * * * * *}", zone = "${app.booking.zone}")
    @Transactional
    public void advanceDueSchedules() {
        var today = policy.today(clock);
        schedules.transitionBeforeDepartureCutoff(RECRUITING_SCHEDULE, CLOSED_SCHEDULE,
            policy.firstBookableDeparture(today));
        // Catch up on finished trips even if the job missed their departure day.
        schedules.transitionEndedSchedules(Set.of(CONFIRMED_SCHEDULE, IN_PROGRESS_SCHEDULE),
            COMPLETED_SCHEDULE, today);
        schedules.transitionBeforeDepartureCutoff(CONFIRMED_SCHEDULE, IN_PROGRESS_SCHEDULE, today.plusDays(1));
    }
}
