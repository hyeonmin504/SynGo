package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import backend.synGo.exception.*;
import backend.synGo.form.requestForm.MySlotForm;
import backend.synGo.form.responseForm.MySlotResponseForm;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.UserRepository;
import backend.synGo.repository.UserSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static backend.synGo.domain.slot.UserSlot.createUserSlot;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlotService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserSlotRepository userSlotRepository;
    private final DateRepository dateRepository;
    private final DateService dateService;


    /**
     * 개인 슬롯을 생성하는 서비스
     * @param mySlotForm
     */
    @Transactional
    public Long createMySlot(MySlotForm mySlotForm, Long userId) {
        if (validDateTime(mySlotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (mySlotForm.getEndDate() != null && mySlotForm.getStartDate().isEqual(mySlotForm.getEndDate()))
            mySlotForm.setEndDate(null);

        LocalDate startDate = mySlotForm.getStartDate().toLocalDate();
        Date date = dateRepository.findDateAndUserSlotByStartDateAndUserId(startDate, userId)   //fetch join
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundUserException("유저 정보 없음"));
                    return new Date(user, startDate);
                }
        );
        //userSlot 생성
        UserSlot userSlot = createUserSlot(
                mySlotForm.getStatus(),
                mySlotForm.getTitle(),
                mySlotForm.getContent(),
                mySlotForm.getStartDate(),
                mySlotForm.getEndDate(),
                mySlotForm.getPlace(),
                mySlotForm.getImportance(),
                date);
        //date의 SlotCount, summary를 업데이트
        dateService.updateDateInfo(date, userSlot);
        // cascade로 전부 저장 전파
        dateRepository.save(date);
        return userSlot.getId();
    }

    /**
     * 개인 슬롯을 검색하는 서비스
     * @param slotId
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public MySlotResponseForm findMySlot(Long slotId, Long userId) {
        //fetch join으로 date, userslot 조회
        Optional<UserSlot> userIdByUserSlotId = userSlotRepository.findUserIdByUserSlotId(slotId);

        if (userIdByUserSlotId.isPresent()) { //date 테이블에 userId 값이 존재하는지 확인
            if (userIdByUserSlotId.get().getDate().getUser().getId().equals(userId)) { // 요청자와 슬롯의 주인이 동일 인물인지 확인
                return createMySlotResponseForm(userIdByUserSlotId.get());
            }
            throw new AccessDeniedException("다른 사용자의 슬롯입니다");
        }
        throw new NotFoundDataException("슬롯이 존재하지 않습니다");
    }

    private static MySlotResponseForm createMySlotResponseForm(UserSlot userSlot) {
        return MySlotResponseForm.builder()
                .title(userSlot.getTitle())
                .content(userSlot.getContent())
                .startDate(userSlot.getStartTime())
                .endDate(userSlot.getEndTime())
                .createDate(userSlot.getCreateDate())
                .place(userSlot.getPlace())
                .status(userSlot.getStatus().getStatus())
                .importance(userSlot.getImportance().getLabel())
                .build();
    }

    public static boolean validDateTime(MySlotForm mySlotForm) {
        return mySlotForm.getEndDate() != null && (
                mySlotForm.getStartDate().isAfter(mySlotForm.getEndDate()) ||
                mySlotForm.getStartDate().isBefore(LocalDateTime.now()) ||
                mySlotForm.getEndDate().isBefore(LocalDateTime.now())
        );
    }
}
