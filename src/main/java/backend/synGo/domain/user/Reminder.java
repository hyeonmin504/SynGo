package backend.synGo.domain.user;

import backend.synGo.domain.slot.user.UserSlot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Long id;

    private String summary;
    private LocalDateTime startTime;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_slot_id")
    private UserSlot userSlot;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
