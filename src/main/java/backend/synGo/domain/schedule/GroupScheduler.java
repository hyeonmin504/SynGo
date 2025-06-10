package backend.synGo.domain.schedule;

import backend.synGo.domain.group.Group;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupScheduler {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_schedule_id")
    private Long id;

    private LocalDateTime lastDeploy;
    @Enumerated(EnumType.STRING)
    private Theme theme;

    @OneToOne(mappedBy = "groupScheduler", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Group group;

    public GroupScheduler(Theme theme) {
        this.lastDeploy = LocalDateTime.now();
        this.theme = theme;
    }

    public void setGroup(Group group) {
        this.group =group;
    }
}
