package com.misterworld.server.repository;

import com.misterworld.server.entity.TravelProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelProductRepository extends JpaRepository<TravelProduct,Long> {

    TravelProduct findById(long id);

}
