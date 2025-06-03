package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.repository.UserSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateService {

    private final UserSlotRepository userSlotRepository;



    /**
     * user이 생성한 date의 SlotCount, summary를 업데이트
     * @param date
     * @param userSlot
     * @return
     */
    public void updateDateInfo(Date date, UserSlot userSlot) {
        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
        if (date.getSlotCount() != 0){
            List<UserSlot> allSlot = date.getUserSlot();
            allSlot.add(userSlot);
            //중요도로 내림차순 정렬한다
            allSlot.sort(Comparator.comparingInt((UserSlot s) -> s.getImportance().getPriority()).reversed());

            //최신화 문제를 예방하기 위해 안전하게 접근
            String summary = allSlot.stream()
                    .limit(2)
                    .map(UserSlot::getTitle)
                    .collect(Collectors.joining(","));

            date.addSlotCountAndSummary(summary);
        }
        else
            date.addSlotCountAndSummary(userSlot.getTitle());
    }
}
