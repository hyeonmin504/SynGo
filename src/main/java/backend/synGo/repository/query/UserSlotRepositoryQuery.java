package backend.synGo.repository.query;

import backend.synGo.form.SlotDtoForDay;

import java.util.List;

public interface UserSlotRepositoryQuery {

    List<SlotDtoForDay> findByUserIdAndDay(Long dateId);
}
