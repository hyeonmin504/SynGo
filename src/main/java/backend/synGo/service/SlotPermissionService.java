package backend.synGo.service;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.slot.Status;
import backend.synGo.exception.NotValidException;
import backend.synGo.repository.SlotPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlotPermissionService {

    private final SlotPermissionRepository slotPermissionRepository;
    public SlotPermission getSlotPermission(String permission) {
        return slotPermissionRepository.findBySlotPermission(permission)
                .orElseThrow(() -> new NotValidException("초기 Permission이 없습니다"));
    }
}
