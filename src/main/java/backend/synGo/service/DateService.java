package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.group.Group;
import backend.synGo.domain.slot.GroupSlot;
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
     * user이 생성한 date의 SlotCount, summary를 업데이트 todo: 그룹 슬롯과 유저 슬롯을 통합해서 summary를 결정하기
     * @param date
     * @param userSlot
     * @return
     */
    @Deprecated // TODO: 6/5/25 user,group 두개 슬롯 포함한 업데이트로 변경
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

    /**
     * user이 생성한 date의 SlotCount, summary를 업데이트
     * @param date
     * @param groupSlot
     * @return
     */
    @Deprecated // TODO: 6/5/25  이건 date 조회 시 설정 하도록 위치 변경 예정
    public void updateGroupDateInfo(Date date, GroupSlot groupSlot) {
        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
        if (date.getSlotCount() != 0){
            log.info("date.getSlotCount()={}",date.getSlotCount());
            List<GroupSlot> allSlot = date.getGroupSlot();
            allSlot.add(groupSlot);
            //중요도로 내림차순 정렬한다
            allSlot.sort(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed());

            //최신화 문제를 예방하기 위해 안전하게 접근
            String summary = allSlot.stream()
                    .limit(2)
                    .map(GroupSlot::getTitle)
                    .collect(Collectors.joining(","));

            date.addSlotCountAndSummary(summary);
        }
        else
            date.addSlotCountAndSummary(groupSlot.getTitle());
    }
}
