package backend.synGo.repository;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSlotRepository extends JpaRepository<UserSlot,Long> {

    List<UserSlot> findByDate(Date date);

}
