package com.misterworld.server.inventory.repository;

import com.misterworld.server.inventory.entity.ItemInventory;


import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface ItemInventoryRepository extends JpaRepository<ItemInventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemInventory i where i.id = :inventoryId")
    Optional<ItemInventory> findByIdForUpdate(@Param("inventoryId") Long inventoryId);
}
