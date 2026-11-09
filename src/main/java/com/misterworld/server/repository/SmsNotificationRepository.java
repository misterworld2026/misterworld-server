package com.misterworld.server.repository;

import com.misterworld.server.domain.*;

import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
    @Query("select n from SmsNotification n where n.travelApplication.member.id = :memberId")
    Page<SmsNotification> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
