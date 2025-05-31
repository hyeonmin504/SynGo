package backend.synGo.service;

import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.user.User;
import backend.synGo.form.responseForm.MySchedulerForm;
import backend.synGo.repository.UserSchedulerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSchedulerService {

    private final UserService userService;
    private final UserSchedulerRepository userSchedulerRepository;

    @Transactional(readOnly = true)
    public MySchedulerForm getMyScheduler(HttpServletRequest request) {
        User user = userService.findUserByToken(request);
        return MySchedulerForm.builder()
                .theme(user.getUserScheduler().getTheme())
                .build();
    }
}
