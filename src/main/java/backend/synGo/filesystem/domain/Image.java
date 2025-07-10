package backend.synGo.filesystem.domain;

import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;
    private String imageName;
    private String imageType;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "image_url_id")
    private ImageUrl imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_slot_id")
    private UserSlot userSlot;

    @Builder
    public Image(String imageName, String imageType, ImageUrl imageUrl, UserSlot userSlot) {
        this.imageName = imageName;
        this.imageType = imageType;
        addImageUrl(imageUrl);
        if (userSlot != null){
            addUserSlot(userSlot);
        }
    }

    private void addUserSlot(UserSlot userSlot) {
        this.userSlot = userSlot;
        userSlot.getImages().add(this);
    }

    private void addImageUrl(ImageUrl imageUrl) {
        this.imageUrl = imageUrl;
        imageUrl.setImage(this);
    }

}
