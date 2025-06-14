package backend.synGo.repository.query;

import backend.synGo.form.DaySlotDto;

import java.util.List;

public interface GroupSlotRepositoryQuery {
    List<DaySlotDto> findByGroupIdAndDay(Long dateId);
    List<DaySlotDto> findDateAndSlotByGroupIdAndDay(Long dateId);
    List<DaySlotDto> findMemberWithUserGroupBySlotIdsAndLeader(List<Long> slotIds);
}
