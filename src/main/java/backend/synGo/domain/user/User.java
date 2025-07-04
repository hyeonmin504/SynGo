package backend.synGo.domain.user;

import backend.synGo.chatBot.domain.ChatMessage;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.userGroupData.UserGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Builder;
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
    private String lastAccessIp;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_scheduler_id")
    private UserScheduler userScheduler;

    @OneToMany(mappedBy = "user")
    private List<UserGroup> userGroups = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Date> date = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ChatMessage> chatMessage = new ArrayList<>();

    // redis 추출 유저 데이터
    public User(String name, String email, String password, String lastAccessIp, UserScheduler scheduler) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.joinDate = LocalDateTime.now();
        this.lastAccessIp = lastAccessIp;
        setUserScheduler(scheduler);
    }

    private void setUserScheduler(UserScheduler scheduler) {
        this.userScheduler = scheduler;
        scheduler.setUser(this);
    }

    public void setNewUserIp(String newUserIp) {
        this.lastAccessIp = newUserIp;
    }

    //Mock test 위한 생성자
    @Builder
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
    @Builder
    public User(String name) {
        this.name = name;
    }
}
