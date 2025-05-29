package backend.synGo.domain.slot;

import backend.synGo.domain.date.Date;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String place;
    private String updateUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_id")
    private Date date;

    @OneToMany(mappedBy = "groupSlot", cascade = CascadeType.ALL)
    private List<SlotMember> slotMember = new ArrayList<>();

    @OneToOne(mappedBy = "groupSlot", fetch = FetchType.LAZY)
    private UserSlot userSlot;
}
