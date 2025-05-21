package backend.synGo.domain.userGroupData;

import lombok.Getter;

@Getter
public enum SlotPermission {
    BASIC("기본"),
    EDITOR("에디터");

    private final String slotPermission;

    SlotPermission(String slotPermission) {
        this.slotPermission = slotPermission;
    }
}
