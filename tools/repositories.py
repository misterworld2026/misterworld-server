from pathlib import Path
base=Path('src/main/java/com/misterworld/server/repository');base.mkdir(parents=True,exist_ok=True)
extra={
'Member': '''Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);''',
'TravelProduct': '''Page<TravelProduct> findByTheme(String theme, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TravelProduct p where p.id = :productId")
    Optional<TravelProduct> findByIdForUpdate(@Param("productId") Long productId);''',
'TravelOption': '''List<TravelOption> findByTravelProductId(Long productId);''',
'TravelSchedule': '''Page<TravelSchedule> findByTravelProductId(Long productId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TravelSchedule s where s.travelScheduleId = :scheduleId")
    Optional<TravelSchedule> findByIdForUpdate(@Param("scheduleId") Long scheduleId);''',
'TravelApplication': '''Page<TravelApplication> findByMemberId(Long memberId, Pageable pageable);
    Optional<TravelApplication> findByIdAndMemberId(Long applicationId, Long memberId);
    @Query("select a from TravelApplication a where a.travelSchedule.travelScheduleId = :scheduleId")
    List<TravelApplication> findByScheduleId(@Param("scheduleId") Long scheduleId);
    @Query("select count(a) > 0 from TravelApplication a where a.travelSchedule.travelScheduleId = :scheduleId")
    boolean existsByScheduleId(@Param("scheduleId") Long scheduleId);
    @Query("select a from TravelApplication a where a.member.id = :memberId and a.status = com.misterworld.server.domain.ApplicationStatus.CONFIRMED and a.travelSchedule.status = com.misterworld.server.domain.ScheduleStatus.COMPLETED_SCHEDULE order by a.travelSchedule.departureDate desc, a.id desc")
    Page<TravelApplication> findCompletedHistoryByMemberId(@Param("memberId") Long memberId, Pageable pageable);''',
'TravelParticipant': '''List<TravelParticipant> findByTravelApplicationId(Long applicationId);''',
'ItemInventory': '''@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemInventory i where i.id = :inventoryId")
    Optional<ItemInventory> findByIdForUpdate(@Param("inventoryId") Long inventoryId);''',
'SmsNotification': '''@Query("select n from SmsNotification n where n.travelApplication.member.id = :memberId")
    Page<SmsNotification> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);'''
}
for entity,methods in extra.items():
 (base/(entity+'Repository.java')).write_text(f'''package com.misterworld.server.repository;
import com.misterworld.server.domain.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
public interface {entity}Repository extends JpaRepository<{entity}, Long> {{
    {methods}
}}
''',encoding='utf-8')
