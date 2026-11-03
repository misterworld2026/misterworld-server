package com.misterworld.server.security;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface TokenSessionRepository extends JpaRepository<TokenSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TokenSession s where s.id = :id")
    Optional<TokenSession> findByIdForUpdate(@Param("id") String id);
}
