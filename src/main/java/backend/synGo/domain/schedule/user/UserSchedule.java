package backend.synGo.domain.schedule.user;

import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.slot.user.UserSlot;
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
public class UserSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    private LocalDateTime lastUpdate;
    @Enumerated(EnumType.STRING)
    private Theme theme;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "userSchedule", cascade = CascadeType.ALL)
    private List<UserSlot> userSlot = new ArrayList<>();
}
