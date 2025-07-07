package backend.synGo.filesystem.domain.image;

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

    @OneToOne(mappedBy = "imageUrl", fetch = FetchType.LAZY)
    private Image image;

    public ImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
