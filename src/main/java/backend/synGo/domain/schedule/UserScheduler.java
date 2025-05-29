package backend.synGo.domain.schedule;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.user.User;
import backend.synGo.form.responseForm.MySchedulerForm;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScheduler {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    private LocalDateTime lastUpdate;
    @Enumerated(EnumType.STRING)
    private Theme theme;

    @OneToOne(mappedBy = "userScheduler", fetch = FetchType.LAZY)
    private User user;

    @OneToMany(mappedBy = "userScheduler", cascade = CascadeType.ALL)
    private List<Date> date = new ArrayList<>();

    public UserScheduler(Theme theme) {
        this.lastUpdate = LocalDateTime.now();
        this.theme = theme;
    }


    public void updateTheme(Theme theme){
        this.theme = theme;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
