package backend.synGo.repository.query.impl;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.form.GroupSlotDto;
import backend.synGo.repository.query.GroupSlotRepositoryQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GroupSlotRepositoryQueryImpl implements GroupSlotRepositoryQuery {

    private final EntityManager em;

    private String query = "select new backend.synGo.form.GroupSlotDto" +
            "(gs.id, gs.title, gs.startTime, gs.endTime, gs.importance) " +
            "from GroupSlot gs " +
            "join gs.date d " +
            "where d.id =:dateId And";
    private String findAllQuery = "select new backend.synGo.form.GroupSlotDto" +
            "(gs.id, gs.title, gs.startTime, gs.endTime, gs.importance, ug.id, ug.nickname) " +
            "from GroupSlot gs " +
            "join gs.date d " +
            "left join gs.slotMember sm on sm.slotPermission = :permission " +
            "left join sm.userGroup ug " +
            "where d.id = :dateId";
    @Override
    public List<GroupSlotDto> findByGroupIdAndDay(Long dateId) {
        return em.createQuery(findAllQuery, GroupSlotDto.class)
                .setParameter("dateId", dateId)
                .setParameter("permission", SlotPermission.EDITOR)
                .getResultList();
    }

    @Override
    public List<GroupSlotDto> findDateAndSlotByGroupIdAndDay(Long dateId) {
        return em.createQuery(query, GroupSlotDto.class)
                .setParameter("dateId", dateId)
                .getResultList();
    }

    @Override
    public List<GroupSlotDto> findMemberWithUserGroupBySlotIdsAndLeader(List<Long> slotIds) {
        return em.createQuery("select new backend.synGo.form.GroupSlotDto" +
                "(sm.groupSlot.id, ug.id, ug.nickname) " +
                "from SlotMember sm " +
                "join sm.userGroup ug " +
                "where sm.groupSlot.id in :slotIds And " +
                "sm.slotPermission= :permission", GroupSlotDto.class)
                .setParameter("slotIds", slotIds)
                .setParameter("permission", SlotPermission.EDITOR)
                .getResultList();
    }
}