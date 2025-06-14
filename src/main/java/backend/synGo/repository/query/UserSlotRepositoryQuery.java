package backend.synGo.repository.query;

import backend.synGo.form.DaySlotDto;

import java.util.List;

public interface UserSlotRepositoryQuery {

    List<DaySlotDto> findByUserIdAndDay(Long dateId);
}
