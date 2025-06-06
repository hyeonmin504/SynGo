package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static backend.synGo.controller.group.UserGroupController.*;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;

    @Transactional
    public void saveUserGroupData(String nickname, User user, Group group, Role role){
        userGroupRepository.save(new UserGroup(nickname, user, group, role));
    }

    @Transactional(readOnly = true)
    public UserGroup findUserGroupByUserIdAndGroupId(Long userId, Long groupId) {
        return userGroupRepository.findByGroupIdAndUserId(userId, groupId).orElseThrow(
                () -> new AccessDeniedException("그룹원 외 접근 불가")
        );
    }


    @Transactional(readOnly = true)
    public List<UserInGroupData> getMember(Long groupId, Long userId) {
        List<UserGroup> userGroups = userGroupRepository.joinUserGroupAndGroupFindAllUserGroupByGroupId(groupId);
        if (checkUserInGroup(userId, userGroups)) {
            return userGroups.stream()
                    .map(userGroup -> UserInGroupData.builder()
                            .userGroupId(userGroup.getId())
                            .role(userGroup.getRole())
                            .joinDate(userGroup.getJoinGroupDate())
                            .nickname(userGroup.getNickname())
                            .build())
                    .collect(Collectors.toList());
        }
        throw new AccessDeniedException("그룹원 외 접근 불가");
    }

    /**
     * 유저가 그룹 내 존재하는지 체크
     * @param requestUserId
     * @param userGroup
     * @return
     */
    public boolean checkUserInGroup(Long requestUserId, List<UserGroup> userGroup) {
        //요청자가 그룹에 이미 가입되어 있는지 확인
        return userGroup.stream()
                .anyMatch(ug -> ug.getUser().getId().equals(requestUserId));
    }
}
