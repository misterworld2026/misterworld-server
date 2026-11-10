package com.misterworld.server.member.repository;

import com.misterworld.server.member.entity.Member;


import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id=:memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);
}
