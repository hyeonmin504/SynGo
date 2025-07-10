package backend.synGo.repository;

import backend.synGo.domain.slot.UserSlot;
import backend.synGo.repository.query.UserSlotRepositoryQuery;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserSlotRepository extends JpaRepository<UserSlot,Long>, UserSlotRepositoryQuery {

    @Query("select us from UserSlot us join fetch us.date d WHERE us.id = :slotId")
    Optional<UserSlot> findDateAndUserSlotByUserSlotId(@Param("slotId") Long slotId);

    @Query("select count(us) > 0 from UserSlot us join us.date d where d.user.id=:userId")
    boolean existUserUserId(Long userId);

    @Query("select us from UserSlot us join fetch us.date d where us.id = :slotId")
    Optional<UserSlot> findSlotWithDateBySlotId(Long slotId);

    @Query("select us from UserSlot us join us.date d where d.user.id = :userId and us.id = :slotId")
    Optional<UserSlot> findByUserIdAndSlotId(@Param("userId") Long userId,
                                             @Param("slotId") Long slotId);
}
