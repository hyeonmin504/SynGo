package backend.synGo.repository;

import backend.synGo.domain.slot.GroupSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupSlotRepository extends JpaRepository<GroupSlot, Long> {

    @Query("select gs from GroupSlot gs left join fetch gs.slotMember sm left join fetch sm.userGroup where gs.id=:slotId")
    Optional<GroupSlot> joinSlotMemberAndUserGroupBySlotId(@Param("slotId") Long slotId);

    @Query("select gs from GroupSlot gs left join fetch gs.slotMember sm where gs.id=:slotId")
    Optional<GroupSlot> joinSlotMemberBySlotId(@Param("slotId") Long slotId);
}
