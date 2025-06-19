package backend.synGo.service;

import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.form.responseForm.SchedulerForm;
import backend.synGo.repository.UserSchedulerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSchedulerService {

    private final UserSchedulerRepository userSchedulerRepository;

    @Transactional(readOnly = true)
    public SchedulerForm getMyScheduler(Long userId) {
        UserScheduler userScheduler = userSchedulerRepository.findSchedulerAndUserByUserId(userId);
        return SchedulerForm.builder()
                .theme(userScheduler.getTheme().getTheme())
                .build();
    }
}
