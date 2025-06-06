package backend.synGo.repository;

import backend.synGo.domain.slot.SlotMember;
import backend.synGo.domain.slot.SlotPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotMemberRepository extends JpaRepository<SlotMember, Long> {
}
