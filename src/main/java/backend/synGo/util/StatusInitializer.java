package backend.synGo.util;

import backend.synGo.domain.slot.Status;
import backend.synGo.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class StatusInitializer implements CommandLineRunner {
    private final StatusRepository statusRepository;
    @Override
    public void run(String... args) {
        initStatus();
    }
    private void initStatus() {
        if (statusRepository.count() == 0) {
            statusRepository.saveAll(List.of(
                    new Status(1L, Status.COMPLETE),
                    new Status(2L, Status.DELAY),
                    new Status(3L, Status.DOING),
                    new Status(4L, Status.PLAN),
                    new Status(5L, Status.CANCEL),
                    new Status(6L, Status.HOLD)
            ));
        }
    }
}