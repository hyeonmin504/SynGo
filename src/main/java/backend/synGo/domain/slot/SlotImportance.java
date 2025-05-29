package backend.synGo.domain.slot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SlotImportance {

    VERY_LOW("매우 낮음", 1),
    LOW("낮음", 2),
    MEDIUM("보통", 3),
    HIGH("높음", 4),
    VERY_HIGH("매우 높음", 5);

    private final String label;
    private final int priority;

    SlotImportance(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static SlotImportance from(String label) {
        for (SlotImportance importance : SlotImportance.values()) {
            if (importance.label.equalsIgnoreCase(label)) {
                return importance;
            }
        }
        throw new IllegalArgumentException("잘못된 SlotImportance 값입니다: " + label);
    }
}