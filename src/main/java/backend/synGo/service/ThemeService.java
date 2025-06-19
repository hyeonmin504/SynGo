package backend.synGo.service;

import backend.synGo.domain.schedule.Theme;
import backend.synGo.exception.NotValidException;
import backend.synGo.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThemeService {
    private final ThemeRepository themeRepository;

    public Theme getTheme(String theme){
        return themeRepository.findByTheme(theme)
                .orElseThrow(() -> new NotValidException("테마 없음"));
    }
}
