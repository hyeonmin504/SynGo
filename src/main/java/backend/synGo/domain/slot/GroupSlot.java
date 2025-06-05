package backend.synGo.domain.slot;

import backend.synGo.domain.date.Date;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.image.PixelGrabber;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_slot_id")
    private Long id;

    @NotNull(message = "status을 null로 할 수 없습니다")
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
    private String place;
    private String updateUser;
    private SlotImportance importance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_id")
    private Date date;

    @OneToMany(mappedBy = "groupSlot", cascade = CascadeType.ALL)
    private List<SlotMember> slotMember = new ArrayList<>();

    public static GroupSlot createGroupSlot(Status status, String title, String content, LocalDateTime startTime, LocalDateTime endTime, String place, SlotImportance importance, Date date) {
        return new GroupSlot(status,title,content,startTime,endTime,place,importance,date);
    }

    public GroupSlot(Status status,String title, String content, LocalDateTime startTime, LocalDateTime endTime, String place, SlotImportance importance, Date date) {
        this.status = status;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createDate = LocalDateTime.now();
        this.place = place;
        this.importance = importance;
        this.updateUser = "Leader";
        setDate(date);
    }

    private void setDate(Date date) {
        this.date = date;
        date.getGroupSlot().add(this);
    }
}
