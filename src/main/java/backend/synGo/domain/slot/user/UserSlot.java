package backend.synGo.domain.slot.user;

import backend.synGo.domain.schedule.user.UserSchedule;
import backend.synGo.domain.slot.Status;
import backend.synGo.domain.slot.group.GroupSlot;
import backend.synGo.domain.user.Reminder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_slot_id")
    private Long id;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;
    @NotNull
    private String title;
    private String content;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private LocalDateTime createDate;
    private String location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_slot_id")
    private GroupSlot groupSlot;

    @OneToOne(mappedBy = "userSlot", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_schedule_id")
    private UserSchedule userSchedule;
}
