package backend.synGo.domain.userGroupData;


import backend.synGo.domain.group.Group;
import backend.synGo.domain.slot.group.SlotMember;
import backend.synGo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_group_id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;
    private LocalDateTime joinGroupDate;
    private LocalDateTime leaveGroupDate;
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(mappedBy = "userGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private SlotMember slotMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;
}
