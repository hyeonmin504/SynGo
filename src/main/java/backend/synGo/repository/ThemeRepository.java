package backend.synGo.repository;

import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.slot.SlotPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByTheme(String theme);
}
