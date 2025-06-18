package backend.synGo.util;

import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.slot.Status;
import backend.synGo.repository.SlotPermissionRepository;
import backend.synGo.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class SlotPermissionInitializer implements CommandLineRunner {
    private final SlotPermissionRepository slotPermissionRepository;
    @Override
    public void run(String... args) {
        initPermission();
    }
    private void initPermission() {
        if (slotPermissionRepository.count() == 0) {
            slotPermissionRepository.saveAll(List.of(
                    new SlotPermission(1L, SlotPermission.EDITOR),
                    new SlotPermission(2L, SlotPermission.BASIC)
            ));
        }
    }
}
