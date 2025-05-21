package backend.synGo.domain.userGroupData;

import lombok.Getter;

@Getter
public enum Role {
    LEADER("리더"),
    MANAGER("매니저"),
    MEMBER("맴버"),
    GUEST("게스트");

    private final String role;

    Role(String role) {
        this.role = role;
    }
}
