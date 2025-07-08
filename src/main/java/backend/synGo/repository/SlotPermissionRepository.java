package backend.synGo.repository;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.slot.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlotPermissionRepository extends JpaRepository<SlotPermission, Long> {
    Optional<SlotPermission> findBySlotPermission(String slotPermission);
}