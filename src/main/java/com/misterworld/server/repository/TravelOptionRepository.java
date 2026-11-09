package com.misterworld.server.repository;

import com.misterworld.server.domain.*;

import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface TravelOptionRepository extends JpaRepository<TravelOption, Long> {
    List<TravelOption> findByTravelProductId(Long productId);
}
