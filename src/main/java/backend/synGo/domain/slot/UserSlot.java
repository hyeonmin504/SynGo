package backend.synGo.domain.slot;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.user.Reminder;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSlot{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_slot_id")
    private Long id;
    @Enumerated(EnumType.STRING)
    private Status status;
    @NotNull
    private String title;
    @Nullable
    private String content;
    @NotNull
    private LocalDateTime startTime;
    @Nullable
    private LocalDateTime endTime;
    @NotNull
    private LocalDateTime createDate;
    @Nullable
    private String place;

    @Enumerated(EnumType.STRING)
    private SlotImportance importance;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_id")
    private Date date;

    private void setDate(Date date) {
        this.date = date;
        date.getUserSlot().add(this);
    }

    public static UserSlot createUserSlot(Status status, String title, String content, LocalDateTime startTime, LocalDateTime endTime, String place, SlotImportance importance, Date date) {
        return new UserSlot(status,title,content,startTime,endTime,place,importance,date);
    }

    public UserSlot(Status status,String title, String content, LocalDateTime startTime, LocalDateTime endTime, String place, SlotImportance importance, Date date) {
        if (status == null) {
                changeStatus(startTime, endTime);
        } else this.status = status;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createDate = LocalDateTime.now();
        this.place = place;
        this.importance = importance;
        setDate(date);
    }

    public void updateSlot(Status status,String title, String content, LocalDateTime startTime, LocalDateTime endTime, String place, SlotImportance importance) {
        this.status = status;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.place = place;
        this.importance = importance;
    }

    //기본 상태 선언
    private void changeStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (endTime.isBefore(now)) {
            this.status = Status.HOLD; // 보류중
        } else if (startTime.isAfter(now)) {
            this.status = Status.PLAN; // 계획중
        } else {
            this.status = Status.DOING; // 진행중
        }
    }
}
