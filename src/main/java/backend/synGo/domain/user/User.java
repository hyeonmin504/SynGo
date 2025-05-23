package backend.synGo.domain.user;

import backend.synGo.domain.schedule.user.UserScheduler;
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
    private String lastAccessIp;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_scheduler_id")
    private UserScheduler userScheduler;

    @OneToMany(mappedBy = "user")
    private List<UserGroup> userGroups = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    // redis 추출 유저 데이터
    public User( String name, String userIp) {
        this.name = name;
        this.lastAccessIp = userIp;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
