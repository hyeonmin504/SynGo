package backend.synGo.domain.user;

import backend.synGo.domain.slot.UserSlot;
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

    @OneToOne(mappedBy = "reminder", fetch = FetchType.LAZY)
    private UserSlot userSlot;

    @OneToOne(mappedBy = "reminder", fetch = FetchType.LAZY)
    private User user;
}
