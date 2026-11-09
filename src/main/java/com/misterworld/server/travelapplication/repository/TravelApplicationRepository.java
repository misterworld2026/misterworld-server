package com.misterworld.server.travelapplication.repository;

import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelschedule.entity.ScheduleStatus;


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

    @EntityGraph(attributePaths = {"travelSchedule", "travelSchedule.travelProduct"})
    @Query("select a from TravelApplication a where a.member.id = :memberId and a.status = com.misterworld.server.travelapplication.entity.ApplicationStatus.CONFIRMED and a.travelSchedule.status = com.misterworld.server.travelschedule.entity.ScheduleStatus.COMPLETED_SCHEDULE order by a.travelSchedule.arrivalDate desc, a.id desc")
    Page<TravelApplication> findCompletedHistoryByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    Optional<TravelApplication> findByMemberIdAndRequestKey(Long memberId, String requestKey);

    @EntityGraph(attributePaths={"travelSchedule", "travelSchedule.travelProduct", "member"})
    @Query("select a from TravelApplication a where a.travelSchedule.travelScheduleId=:scheduleId and a.status<>com.misterworld.server.travelapplication.entity.ApplicationStatus.CANCELLED order by a.id")
    List<TravelApplication> findValidApplicationsByScheduleId(@Param("scheduleId") Long scheduleId);

    @EntityGraph(attributePaths={"travelSchedule", "travelSchedule.travelProduct", "member"})
    @Query("select a from TravelApplication a where a.travelSchedule.travelScheduleId=:scheduleId and a.status=com.misterworld.server.travelapplication.entity.ApplicationStatus.CONFIRMED order by a.id")
    List<TravelApplication> findNotificationTargets(@Param("scheduleId") Long scheduleId);

    @Query("select count(distinct a.travelSchedule.travelScheduleId) from TravelApplication a where a.member.id=:memberId and a.status=com.misterworld.server.travelapplication.entity.ApplicationStatus.CONFIRMED and a.travelSchedule.status=com.misterworld.server.travelschedule.entity.ScheduleStatus.COMPLETED_SCHEDULE")
    long countCompletedTrips(@Param("memberId") Long memberId);

    @Query("select count(a)>0 from TravelApplication a where a.travelSchedule.travelProduct.id=:productId")
    boolean existsByProductId(@Param("productId") Long productId);

    default void confirmPendingApplications(Long scheduleId) {
        findValidApplicationsByScheduleId(scheduleId).stream()
            .filter(a -> a.getStatus()==ApplicationStatus.PENDING)
            .forEach(a -> a.setStatus(ApplicationStatus.CONFIRMED));
    }
}
