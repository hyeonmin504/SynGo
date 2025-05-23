package backend.synGo.domain.schedule.group;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.slot.group.GroupSlot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "groupScheduler", cascade = CascadeType.ALL)
    private List<GroupSlot> groupSlot = new ArrayList<>();


}
