package com.misterworld.server.inventory.repository;

import com.misterworld.server.inventory.entity.InventoryDeduction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryDeductionRepository extends JpaRepository<InventoryDeduction, Long> {
    List<InventoryDeduction> findByScheduleId(Long scheduleId);

    List<InventoryDeduction> findByScheduleIdAndRestoredAtIsNullOrderByInventoryId(Long scheduleId);
}
