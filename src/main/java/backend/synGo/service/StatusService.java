package backend.synGo.service;

import backend.synGo.domain.slot.Status;
import backend.synGo.exception.NotValidException;
import backend.synGo.repository.StatusRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatusService {
    private final StatusRepository statusRepository;

    public Status getStatus(String status) {
        return statusRepository.findByStatus(status)
                .orElseThrow(() -> new NotValidException("초기 Status가 없습니다"));
    }

}
