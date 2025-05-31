package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.schedule.GroupScheduler;
import backend.synGo.repository.GroupSchedulerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupSchedulerService {

    private final GroupSchedulerRepository groupSchedulerRepository;

    @Transactional
    public GroupScheduler createScheduler(Group group) {
        return groupSchedulerRepository.save(
                new GroupScheduler(group)
        );
    }
}
