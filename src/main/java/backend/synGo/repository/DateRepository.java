package backend.synGo.repository;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.user.User;
import backend.synGo.repository.query.DateRepositoryQuery;
import backend.synGo.service.GroupSlotService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DateRepository extends JpaRepository<Date, Long>, DateRepositoryQuery {
    Optional<Date> findByStartDateAndUser(LocalDate startDate, User user);

    @Query("select d from scheduleDate d join fetch d.userSlot where d.user.id=:userId AND d.startDate=:startDate")
    Optional<Date> findDateAndUserSlotByStartDateAndUserId(@Param("startDate") LocalDate startDate,@Param("userId") Long userId);

    @Query("select d from scheduleDate d join fetch d.groupSlot where d.group.id=:groupId AND d.startDate=:startDate")
    Optional<Date> findDateAndGroupSlotByStartDateAndUserId(@Param("startDate") LocalDate startDate,@Param("groupId") Long groupId);

    @Query("select distinct d from scheduleDate d join fetch d.groupSlot gs where d.group.id = :groupId and d.startDate >= :startDate and d.startDate < :endDate")
    List<Date> findScheduleDateWithSlotsByGroupAndMonthRange(
            @Param("groupId") Long groupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("select d from scheduleDate d where d.group.id= :groupId and d.startDate= :day")
    Optional<Date> findByGroupIdAndDay(@Param("groupId") Long groupId, @Param("day") LocalDate day);
}
