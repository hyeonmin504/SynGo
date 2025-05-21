package backend.synGo.domain.schedule;

import lombok.Getter;

@Getter
public enum Theme {
    BLACK("블랙"), WHITE("화이트");

    private final String theme;

    Theme(String theme) {
        this.theme = theme;
    }
}
