package backend.synGo.domain.group;

import backend.synGo.domain.schedule.group.GroupScheduler;
import backend.synGo.domain.userGroupData.UserGroup;
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
@Table(name = "group_basic")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String password;
    private LocalDateTime createDate;
    @Enumerated(EnumType.STRING)
    private GroupType groupType;
    private String name;
    private String information;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<UserGroup> userGroup = new ArrayList<>();

    @OneToOne(mappedBy = "group", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private GroupScheduler groupScheduler;
}
