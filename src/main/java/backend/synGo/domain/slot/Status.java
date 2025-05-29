package backend.synGo.domain.slot;

import lombok.Getter;

@Getter
public enum Status {
    COMPLETE("완료"),
    DELAY("지연"),
    DOING("진행중"),
    PLAN("계획중"),
    CANCEL("취소"),
    HOLD("보류");

    private final String status;


    Status(String status) {
        this.status = status;
    }
}
