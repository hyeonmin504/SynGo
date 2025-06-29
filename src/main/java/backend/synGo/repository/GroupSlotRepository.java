package backend.synGo.repository;

import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.repository.query.GroupSlotRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupSlotRepository extends JpaRepository<GroupSlot, Long>, GroupSlotRepositoryQuery {

    @Query("select gs from GroupSlot gs left join fetch gs.status s left join fetch gs.slotMember sm left join fetch sm.slotPermission sp left join fetch sm.userGroup where gs.id=:slotId")
    Optional<GroupSlot> joinSlotMemberAndUserGroupBySlotId(@Param("slotId") Long slotId);

    @Query("select gs from GroupSlot gs left join fetch gs.slotMember sm where gs.id=:slotId")
    Optional<GroupSlot> joinSlotMemberBySlotId(@Param("slotId") Long slotId);

    @Query("select gs from GroupSlot gs join fetch gs.status s where gs.date.id in :dateIds")
    List<GroupSlot> findByDateIdIn(@Param("dateIds") List<Long> dateIds);

    @Query("select gs from GroupSlot gs join fetch gs.date d where gs.id = :slotId")
    Optional<GroupSlot> findSlotWithDateBySlotId(Long slotId);
}
