package backend.synGo.repository.query;

import backend.synGo.form.GroupSlotDto;

import java.util.List;

public interface GroupSlotRepositoryQuery {
    List<GroupSlotDto> findByGroupIdAndDay(Long dateId);
    List<GroupSlotDto> findDateAndSlotByGroupIdAndDay(Long dateId);
    List<GroupSlotDto> findMemberWithUserGroupBySlotIdsAndLeader(List<Long> slotIds);
}
