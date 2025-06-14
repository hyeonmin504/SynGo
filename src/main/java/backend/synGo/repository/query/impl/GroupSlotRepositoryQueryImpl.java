package backend.synGo.repository.query.impl;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.form.DaySlotDto;
import backend.synGo.repository.query.GroupSlotRepositoryQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GroupSlotRepositoryQueryImpl implements GroupSlotRepositoryQuery {

    private final EntityManager em;

    private String query = "select new backend.synGo.form.DaySlotDto" +
            "(gs.id, gs.title, gs.startTime, gs.endTime, gs.importance) " +
            "from GroupSlot gs " +
            "join gs.date d " +
            "where d.id =:dateId And";
    private String findAllQuery = "select new backend.synGo.form.DaySlotDto" +
            "(gs.id, gs.title, gs.startTime, gs.endTime, gs.importance, ug.id, ug.nickname) " +
            "from GroupSlot gs " +
            "join gs.date d " +
            "left join gs.slotMember sm on sm.slotPermission = :permission " +
            "left join sm.userGroup ug " +
            "where d.id = :dateId";
    @Override
    public List<DaySlotDto> findByGroupIdAndDay(Long dateId) {
        return em.createQuery(findAllQuery, DaySlotDto.class)
                .setParameter("dateId", dateId)
                .setParameter("permission", SlotPermission.EDITOR)
                .getResultList();
    }

    @Override
    public List<DaySlotDto> findDateAndSlotByGroupIdAndDay(Long dateId) {
        return em.createQuery(query, DaySlotDto.class)
                .setParameter("dateId", dateId)
                .getResultList();
    }

    @Override
    public List<DaySlotDto> findMemberWithUserGroupBySlotIdsAndLeader(List<Long> slotIds) {
        return em.createQuery("select new backend.synGo.form.DaySlotDto" +
                "(sm.groupSlot.id, ug.id, ug.nickname) " +
                "from SlotMember sm " +
                "join sm.userGroup ug " +
                "where sm.groupSlot.id in :slotIds And " +
                "sm.slotPermission= :permission", DaySlotDto.class)
                .setParameter("slotIds", slotIds)
                .setParameter("permission", SlotPermission.EDITOR)
                .getResultList();
    }
}