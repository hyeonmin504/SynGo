package backend.synGo.repository.query;

import backend.synGo.form.SlotDtoForDay;

import java.util.List;

public interface GroupSlotRepositoryQuery {
    // 그룹 내 하루 슬롯 데이터 정보 요청 query
    List<SlotDtoForDay> findByGroupIdAndDay(Long dateId);
    List<SlotDtoForDay> findDateAndSlotByGroupIdAndDay(Long dateId);

    // 개인의 하루 그룹 slot 데이터 정보 요청 query
    List<SlotDtoForDay> findWithMemberAndUserGroupByDateIdIn(List<Long> dateIds);
}
