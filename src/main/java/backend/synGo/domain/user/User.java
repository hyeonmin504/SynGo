package backend.synGo.domain.user;

import backend.synGo.domain.schedule.user.UserSchedule;
import backend.synGo.domain.userGroupData.UserGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String name;
    @Email
    private String email;
    private String password;
    private LocalDateTime joinDate;
    private LocalDateTime lastDate;
    private String recentAccessIp;
    private String lastAccessIp;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSchedule userSchedule;

    @OneToMany(mappedBy = "user")
    private List<UserGroup> userGroups = new ArrayList<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Reminder reminder;
}
