package com.misterworld.server.repository;

import com.misterworld.server.entity.TravelSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelScheduleRepository extends JpaRepository<TravelSchedule, Long> {
}
