package backend.synGo.domain.slot.group;

import backend.synGo.domain.userGroupData.SlotPermission;
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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_slot_id")
    private GroupSlot groupSlot;
}
