package com.misterworld.server.repository;

import com.misterworld.server.domain.*;

import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface TravelScheduleRepository extends JpaRepository<TravelSchedule, Long> {
    Page<TravelSchedule> findByTravelProductId(Long productId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TravelSchedule s where s.travelScheduleId = :scheduleId")
    Optional<TravelSchedule> findByIdForUpdate(@Param("scheduleId") Long scheduleId);
}
