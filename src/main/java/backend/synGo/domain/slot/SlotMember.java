package backend.synGo.domain.slot;

import backend.synGo.domain.userGroupData.UserGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private SlotPermission slotPermission;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_slot_id")
    private GroupSlot groupSlot;

    public SlotMember(SlotPermission slotPermission, UserGroup userGroup, GroupSlot groupSlot) {
        this.slotPermission = slotPermission;
        setUserGroup(userGroup);
        setGroupSlot(groupSlot);
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
        userGroup.getSlotMember().add(this);
    }

    public void setGroupSlot(GroupSlot groupSlot) {
        this.groupSlot = groupSlot;
        groupSlot.getSlotMember().add(this);
    }
}
