package com.misterworld.server.travelproduct.repository;

import com.misterworld.server.travelproduct.entity.TravelProduct;


import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface TravelProductRepository extends JpaRepository<TravelProduct, Long> {
    Page<TravelProduct> findByTheme(String theme, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TravelProduct p where p.id = :productId")
    Optional<TravelProduct> findByIdForUpdate(@Param("productId") Long productId);
}
