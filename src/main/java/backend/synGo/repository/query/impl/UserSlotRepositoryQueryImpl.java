package backend.synGo.repository.query.impl;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.form.DaySlotDto;
import backend.synGo.repository.query.UserSlotRepositoryQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserSlotRepositoryQueryImpl implements UserSlotRepositoryQuery {

    private final EntityManager em;

    private String findAllQuery = "select new backend.synGo.form.DaySlotDto" +
            "(gs.id, gs.title, gs.startTime, gs.endTime, gs.importance, ug.id, ug.nickname) " +
            "from GroupSlot gs " +
            "join gs.date d " +
            "left join gs.slotMember sm on sm.slotPermission = :permission " +
            "left join sm.userGroup ug " +
            "where d.id = :dateId";
    @Override
    public List<DaySlotDto> findByUserIdAndDay(Long dateId) {
        return em.createQuery(findAllQuery, DaySlotDto.class)
                .setParameter("dateId", dateId)
                .setParameter("permission", SlotPermission.EDITOR)
                .getResultList();
    }
}
