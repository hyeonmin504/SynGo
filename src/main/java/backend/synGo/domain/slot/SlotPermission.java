package backend.synGo.domain.slot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
public class SlotPermission {
    public static final String BASIC = "BASIC";
    public static final String EDITOR = "EDITOR";

    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    @NotBlank
    private String slotPermission;

    @OneToMany(mappedBy = "slotPermission")
    private List<SlotMember> slotMember = new ArrayList<>();

    public SlotPermission(Long id, String permission) {
        this.id = id;
        this.slotPermission = permission;
    }
}
