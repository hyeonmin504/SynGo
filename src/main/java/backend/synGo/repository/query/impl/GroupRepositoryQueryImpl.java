package backend.synGo.repository.query.impl;

import backend.synGo.domain.userGroupData.Role;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.repository.query.GroupRepositoryQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryQueryImpl implements GroupRepositoryQuery {

    private final EntityManager em;

    @Override
    public Slice<GroupsPagingForm> findAllGroupForSlice(Pageable pageable) {
        try {
            Role role = Role.LEADER;
            TypedQuery<GroupsPagingForm> query = em.createQuery(
                    "select new backend.synGo.form.GroupsPagingForm" +
                            "(g.id, ug.id, g.createDate, g.name, g.information, ug.nickname) " +
                            "from GroupBasic g " +
                            "join g.userGroup ug " +
                            "where ug.role = :role", GroupsPagingForm.class);

            query.setParameter("role", role);
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());

            List<GroupsPagingForm> resultList = query.getResultList();

            boolean hasNext = resultList.size() == pageable.getPageSize();

            return new SliceImpl<>(resultList, pageable, hasNext);
        } catch (NoResultException | EmptyResultDataAccessException e) {
            throw new NotFoundContentsException("생성된 컨텐츠가 없습니다");
        }
    }
}
