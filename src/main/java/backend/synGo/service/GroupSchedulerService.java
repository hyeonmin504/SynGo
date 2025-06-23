package backend.synGo.service;

import backend.synGo.domain.schedule.GroupScheduler;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.form.responseForm.SchedulerForm;
import backend.synGo.repository.GroupSchedulerRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupSchedulerService {

    private final GroupSchedulerRepository groupSchedulerRepository;
    private final UserGroupRepository userGroupRepository;
    private final ThemeService themeService;

    @Transactional
    public GroupScheduler createScheduler(String theme) {

        return new GroupScheduler(
                themeService.getTheme(theme)
        );
    }

    @Transactional(readOnly = true)
    public SchedulerForm getMyScheduler(Long userId,Long groupId) {
        if (userGroupRepository.existsByGroupIdAndUserId(groupId,userId)) {
            GroupScheduler groupScheduler = groupSchedulerRepository.findSchedulerAndGroupByGroupId(groupId);
            return SchedulerForm.builder()
                    .theme(groupScheduler.getTheme().getTheme())
                    .build();
        }
        throw new AccessDeniedException("그룹원 외 접근 불가");

    }
}
