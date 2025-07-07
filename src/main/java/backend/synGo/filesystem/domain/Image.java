package backend.synGo.filesystem.domain;

import backend.synGo.domain.user.User;
import backend.synGo.filesystem.domain.image.ImageUrl;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Image(String imageName, String imageType, ImageUrl imageUrl, User user) {
        this.imageName = imageName;
        this.imageType = imageType;
        addImageUrl(imageUrl);
        addUser(user);
    }

    private void addImageUrl(ImageUrl imageUrl) {
        this.imageUrl = imageUrl;
        imageUrl.setImage(this);
    }

    private void addUser(User user) {
        this.user = user;
        user.getImages().add(this);
    }

}
