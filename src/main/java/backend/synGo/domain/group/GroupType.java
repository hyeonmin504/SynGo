package backend.synGo.domain.group;

import lombok.Getter;

@Getter
public enum GroupType {
    BASIC("기본"),
    RESERVATION("예약");

    private final String groupType;

    GroupType(String groupType) {
        this.groupType = groupType;
    }
}
