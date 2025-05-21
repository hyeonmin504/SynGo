package backend.synGo.domain.slot;

import lombok.Getter;

@Getter
public enum Status {
    PENDING("미완료"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소"),
    ON_HOLD("보류");

    private final String status;

    Status(String status) {
        this.status = status;
    }
}
