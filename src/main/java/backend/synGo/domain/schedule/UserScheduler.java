package backend.synGo.domain.schedule;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.user.User;
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
public class UserScheduler {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    private LocalDateTime lastUpdate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
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
