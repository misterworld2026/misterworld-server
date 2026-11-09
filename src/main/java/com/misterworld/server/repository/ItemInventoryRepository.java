package com.misterworld.server.repository;

import com.misterworld.server.domain.*;

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
