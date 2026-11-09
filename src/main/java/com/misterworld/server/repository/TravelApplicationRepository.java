package com.misterworld.server.repository;

import com.misterworld.server.domain.*;

import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface TravelApplicationRepository extends JpaRepository<TravelApplication, Long> {
    Page<TravelApplication> findByMemberId(Long memberId, Pageable pageable);

    Optional<TravelApplication> findByIdAndMemberId(Long applicationId, Long memberId);

    @Query("select a from TravelApplication a where a.travelSchedule.travelScheduleId = :scheduleId")
    List<TravelApplication> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("select count(a) > 0 from TravelApplication a where a.travelSchedule.travelScheduleId = :scheduleId")
    boolean existsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("select a from TravelApplication a where a.member.id = :memberId and a.status = com.misterworld.server" +
        ".domain.ApplicationStatus.CONFIRMED and a.travelSchedule.status = com.misterworld.server.domain" +
        ".ScheduleStatus.COMPLETED_SCHEDULE order by a.travelSchedule.departureDate desc, a.id desc")
    Page<TravelApplication> findCompletedHistoryByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
