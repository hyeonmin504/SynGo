package backend.synGo.domain.slot;

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
