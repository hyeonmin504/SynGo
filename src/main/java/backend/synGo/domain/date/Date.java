package backend.synGo.domain.date;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "ScheduleDate")
@Table(name = "schedule_date")
public class Date {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "date_id")
    private Long id;

    private int slotCount;
    private LocalDate startDate;

    @OneToMany(mappedBy = "date", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<UserSlot> userSlot = new ArrayList<>();
    @OneToMany(mappedBy = "date", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupSlot> groupSlot = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_scheduler_id")
    private UserScheduler userScheduler;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    public Date(User user, LocalDate startDate) {
        setUser(user);
        this.startDate = startDate;
        slotCount = 0;
    }

    public Date(Group group, LocalDate startDate) {
        setGroup(group);
        this.startDate = startDate;
        slotCount = 0;
    }

    public void addSlotCount(){
        this.slotCount ++;
    }

    // 양방향 연관 관계
    public void setUser(User user){
        this.user = user;
        user.getDate().add(this);
    }

    public void setGroup(Group group){
        this.group = group;
        group.getDate().add(this);
    }

    public void setUserScheduler(UserScheduler userScheduler){
        this.userScheduler = userScheduler;
        userScheduler.getDate().add(this);
    }


    public void setGroupSlot(List<GroupSlot> slots) {
        this.groupSlot = slots;
    }

    public void removeGroupSlot(GroupSlot groupSlot) {
        if (this.groupSlot.contains(groupSlot)) {
            groupSlot.removeDate(this); // 연결 해제
            slotCount--; // 슬롯 카운트 감소
        }
    }
}
