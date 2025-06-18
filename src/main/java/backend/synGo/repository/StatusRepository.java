package backend.synGo.repository;

import backend.synGo.domain.slot.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByStatus(String Status);
}
