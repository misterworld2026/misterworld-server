package com.misterworld.server.travelschedule.service;

import com.misterworld.server.travelapplication.service.BookingPolicy;
import com.misterworld.server.common.BusinessException;
import com.misterworld.server.repository.TravelScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

import static com.misterworld.server.domain.ScheduleStatus.*;

@Service
@RequiredArgsConstructor
public class ScheduleStatusScheduler {
    private static final Logger log = LoggerFactory.getLogger(ScheduleStatusScheduler.class);
    private final TravelScheduleRepository schedules;
    private final TravelScheduleService service;
    private final BookingPolicy policy;
    private final Clock clock;

    @Scheduled(cron = "${app.schedule.transition-cron:0 * * * * *}", zone = "${app.booking.zone}")
    public void advanceDueSchedules() {
        var today = policy.today(clock);
        schedules.transitionBeforeDepartureCutoff(RECRUITING_SCHEDULE, CLOSED_SCHEDULE,
            policy.firstBookableDeparture(today));
        // Stock is deducted when each reservation is paid.
        for (Long scheduleId : schedules.findDueScheduleIds(CONFIRMED_SCHEDULE, today)) {
            try {
                service.startTravel(scheduleId);
            } catch (BusinessException e) {
                log.warn("Could not start schedule {}: {}", scheduleId, e.getCode());
            }
        }
        // Catch up on finished trips even if the job missed their end day.
        schedules.transitionEndedSchedules(Set.of(IN_PROGRESS_SCHEDULE), COMPLETED_SCHEDULE, today);
    }
}
