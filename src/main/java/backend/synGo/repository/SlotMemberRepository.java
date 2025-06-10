package backend.synGo.repository;

import backend.synGo.domain.slot.SlotMember;
import backend.synGo.repository.query.SlotMemberRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotMemberRepository extends JpaRepository<SlotMember, Long>, SlotMemberRepositoryQuery {
}