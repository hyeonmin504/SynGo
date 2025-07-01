package backend.synGo.domain.image;

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

    private Long imageUrl;

    @OneToOne(mappedBy = "imageUrl", fetch = FetchType.LAZY)
    private Image image;
}
