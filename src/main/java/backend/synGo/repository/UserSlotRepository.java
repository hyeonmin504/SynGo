package backend.synGo.repository;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserSlotRepository extends JpaRepository<UserSlot,Long> {

    List<UserSlot> findByDate(Date date);

    @Query("SELECT d.user.id FROM UserSlot us JOIN us.date d WHERE us.id = :slotId")
    Optional<Long> findUserIdByUserSlotId(@Param("slotId") Long slotId);
}
