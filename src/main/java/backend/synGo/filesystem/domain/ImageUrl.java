package backend.synGo.filesystem.domain;

import backend.synGo.domain.user.User;
import backend.synGo.filesystem.domain.Image;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_url_id")
    private Long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(mappedBy = "imageUrl", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Image image;

    public ImageUrl(String imageUrl, User user) {
        this.imageUrl = imageUrl;
        addUser(user);
    }

    private void addUser(User user) {
        this.user = user;
        user.getImageUrls().add(this);
    }

    public ImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
