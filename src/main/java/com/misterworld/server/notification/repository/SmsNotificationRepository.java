package com.misterworld.server.notification.repository;

import com.misterworld.server.notification.entity.NotificationType;
import com.misterworld.server.notification.entity.SmsNotification;


import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
    @Query("select n from SmsNotification n where n.travelApplication.member.id = :memberId")
    Page<SmsNotification> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    boolean existsByTravelApplicationIdAndNotificationType(Long applicationId,
        NotificationType notificationType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from SmsNotification n where n.id=:notificationId")
    Optional<SmsNotification> findByIdForUpdate(@Param("notificationId") Long notificationId);

    @Query("""
        select n.id from SmsNotification n
        where n.status <> com.misterworld.server.notification.entity.NotificationStatus.SENT
          and (n.lastError is null or n.lastError <> :heldError)
          and (n.nextAttemptAt is null or n.nextAttemptAt <= :now)
          and (n.lockedUntil is null or n.lockedUntil <= :now)
        order by n.id
        """)
    List<Long> findDue(@Param("now") java.time.LocalDateTime now, @Param("heldError") String heldError, Pageable pageable);
}
