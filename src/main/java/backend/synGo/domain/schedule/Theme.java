package backend.synGo.domain.schedule;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {
    public static final String BLACK = "BLACK";
    public static final String WHITE = "WHITE";
    @Id
    @Column(name = "theme_id")
    private Long id;
    private String theme;
    @OneToMany(mappedBy = "theme")
    private List<UserScheduler> userSchedulers = new ArrayList<>();
    @OneToMany(mappedBy = "theme")
    private List<GroupScheduler> groupSchedulers = new ArrayList<>();

    public Theme(Long id, String theme) {
        this.id = id;
        this.theme = theme;
    }
}
