package backend.synGo.util;

import backend.synGo.domain.schedule.Theme;
import backend.synGo.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
@RequiredArgsConstructor
public class ThemeInitializer implements CommandLineRunner {
    private final ThemeRepository themeRepository;
    @Override
    public void run(String... args) {
        initTheme();
    }

    private void initTheme() {
        if (themeRepository.count() == 0) {
            themeRepository.saveAll(List.of(
                    new Theme(1L, Theme.BLACK),
                    new Theme(2L, Theme.WHITE)
            ));
        }
    }
}
