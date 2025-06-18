package backend.synGo.domain.slot;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Status {

    public static final String PLAN = "PLAN";
    public static final String DOING = "DOING";
    public static final String COMPLETE = "COMPLETE";
    public static final String CANCEL = "CANCEL";
    public static final String DELAY = "DELAY";
    public static final String HOLD = "HOLD";

    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    @NotBlank
    private String status;

    public Status(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    private List<UserSlot> userSlot = new ArrayList<>();
    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    private List<GroupSlot> groupSlot = new ArrayList<>();
}
