package backend.synGo.domain.group;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.schedule.GroupScheduler;
import backend.synGo.domain.userGroupData.UserGroup;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "GroupBasic")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_basic")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Nullable
    private String password;
    private LocalDateTime createDate;
    @Enumerated(EnumType.STRING)
    private GroupType groupType;
    private String name;
    private String information;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroup> userGroup = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "group_scheduler_id")
    private GroupScheduler groupScheduler;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Date> date = new ArrayList<>();

    public Group( GroupType groupType, String name, String information) {
        this.createDate = LocalDateTime.now();
        this.groupType = groupType;
        this.name = name;
        this.information = information;
    }

    public Group(String password, GroupType groupType, String name, String information) {
        this.password = password;
        this.createDate = LocalDateTime.now();
        this.groupType = groupType;
        this.name = name;
        this.information = information;
    }

    //Mock test 위한 setter
    @Builder
    public void setId(Long id) {
        this.id = id;
    }
}
