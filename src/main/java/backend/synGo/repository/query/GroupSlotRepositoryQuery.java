package backend.synGo.repository.query;

import backend.synGo.form.SlotDtoForDay;

import java.util.List;

public interface GroupSlotRepositoryQuery {
    List<SlotDtoForDay> findByGroupIdAndDay(Long dateId);
    List<SlotDtoForDay> findDateAndSlotByGroupIdAndDay(Long dateId);
    List<SlotDtoForDay> findMemberWithUserGroupBySlotIdsAndLeader(List<Long> slotIds);
    List<SlotDtoForDay> findWithMemberAndUserGroupByDateIdIn(List<Long> dateIds);
}
