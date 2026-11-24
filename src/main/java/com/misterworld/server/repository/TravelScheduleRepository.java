package com.misterworld.server.repository;

import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;


import java.time.LocalDate;
import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface TravelScheduleRepository extends JpaRepository<TravelSchedule, Long> {
    @Override
    @EntityGraph(attributePaths = "travelProduct")
    Page<TravelSchedule> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "travelProduct")
    Page<TravelSchedule> findByTravelProductId(Long productId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TravelSchedule s where s.travelScheduleId = :scheduleId")
    Optional<TravelSchedule> findByIdForUpdate(@Param("scheduleId") Long scheduleId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
        update TravelSchedule s set s.status = :next
        where s.status = :current and s.departureDate < :cutoff
        """)
    void transitionBeforeDepartureCutoff(@Param("current") ScheduleStatus current,
                                         @Param("next") ScheduleStatus next,
                                         @Param("cutoff") LocalDate cutoff);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
        update TravelSchedule s set s.status = :next
        where s.status in :current and s.arrivalDate < :today
        """)
    void transitionEndedSchedules(@Param("current") Set<ScheduleStatus> current,
                                  @Param("next") ScheduleStatus next,
                                  @Param("today") LocalDate today);

    @Query("select s.travelScheduleId from TravelSchedule s where s.status = :status and s.departureDate <= :today order by s.travelScheduleId")
    List<Long> findDueScheduleIds(@Param("status") ScheduleStatus status, @Param("today") LocalDate today);
}
