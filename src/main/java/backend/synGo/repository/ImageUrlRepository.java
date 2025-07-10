package backend.synGo.repository;

import backend.synGo.filesystem.domain.Image;
import backend.synGo.filesystem.domain.ImageUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ImageUrlRepository extends JpaRepository<ImageUrl, Long> {

    @Query("select iu from ImageUrl iu join fetch iu.image i where iu.user.id = :userId and i.userSlot.id = :slotId and iu.imageUrl in :imageUrls")
    List<ImageUrl> findByMyImageUrl(Long userId, Long slotId, List<String> imageUrls);
}
