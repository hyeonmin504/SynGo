package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;

    @Transactional
    public UserGroup saveUserGroupData(String nickname, User user, Group group, Role role){

        return userGroupRepository.save(new UserGroup(nickname, user, group, role));
    }

    @Transactional(readOnly = true)
    public UserGroup findGroupByUserGropId(Long userGroupId) {
        return userGroupRepository.findById(userGroupId)
                .orElseThrow(() -> new NotFoundContentsException("해당 그룹을 찾을 수 없습니다."));
    }
}
