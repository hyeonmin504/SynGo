package backend.synGo.domain.user;

import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.chatBot.domain.ChatHistory;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.filesystem.domain.ImageUrl;
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
    private String password;    //OAuth 사용자의 경우 null
    private LocalDateTime joinDate;
    private String lastAccessIp;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_oauth_id")
    private UserOAuthConnection userOAuthConnection;

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

    @OneToMany(mappedBy = "user")
    private List<ChatHistory> chatHistory = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ImageUrl> imageUrls = new ArrayList<>();

    // redis 추출 유저 데이터
    @Builder
    public User(String name, String email, String password, String lastAccessIp, UserScheduler scheduler, UserOAuthConnection userOAuthConnection) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.joinDate = LocalDateTime.now();
        this.lastAccessIp = lastAccessIp;
        setUserScheduler(scheduler);
        if (userOAuthConnection == null){
            this.userOAuthConnection = null;
        }
        else setUserOAuthConnection(userOAuthConnection);
    }

    private void setUserScheduler(UserScheduler scheduler) {
        this.userScheduler = scheduler;
        scheduler.setUser(this);
    }

    public void setUserOAuthConnection(UserOAuthConnection userOAuthConnection) {
        this.userOAuthConnection = userOAuthConnection;
        userOAuthConnection.getUser().add(this);
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

    // OAuth2 정보 업데이트
    public void updateOAuth2Info(String name, String lastAccessIp) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        this.lastAccessIp = lastAccessIp;
    }
}
