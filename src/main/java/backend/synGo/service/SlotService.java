package backend.synGo.service;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundDataException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.MySlotForm;
import backend.synGo.form.responseForm.MySlotResponseForm;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.UserSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static backend.synGo.domain.slot.UserSlot.createUserSlot;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlotService {

    private final UserService userService;
    private final UserSlotRepository userSlotRepository;
    private final DateRepository dateRepository;
    private final DateService dateService;


    /**
     * 개인 슬롯을 생성하는 서비스
     * @param mySlotForm
     */
    @Transactional
    public Long createMySlot(MySlotForm mySlotForm, CustomUserDetails userDetails) {
        if (validDateTime(mySlotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (mySlotForm.getEndDate() != null && mySlotForm.getStartDate().isEqual(mySlotForm.getEndDate()))
            mySlotForm.setEndDate(null);

        //user data 찾기
        User user = userService.findUserById(userDetails.getUserId());

        //date에 저장 용도로 LocalDateTime -> LocalDate 변환
        LocalDate startDate = mySlotForm.getStartDate().toLocalDate();

        //일정을 저장하려는 날에 대한 데이터가 있는지 확인합니다. 없으면 생성합니다
        Optional<Date> optionalDate = dateRepository.findByStartDateAndUser(startDate, user);

        boolean isNewDate = false;
        Date date;
        //Date가 수정인지 생성인지 구별
        if (optionalDate.isPresent()) {
            date = optionalDate.get();
        } else {
            date = new Date(user ,startDate);
            isNewDate = true;
        }
        //userSlot 생성
        UserSlot userSlot = createUserSlot(mySlotForm.getStatus(), mySlotForm.getTitle(), mySlotForm.getContent(), mySlotForm.getStartDate(), mySlotForm.getEndDate(), mySlotForm.getPlace(), mySlotForm.getImportance(), date);

        //date의 SlotCount, summary를 업데이트
        Date updatedDate = dateService.updateDateInfo(date, userSlot);
        // cascade로 전부 저장 전파
        if (updatedDate.getSlotCount() == 1) dateRepository.save(date);
        else userSlotRepository.save(userSlot);
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
        //데이터의 주인 Id 값 가져오기
        Optional<Long> userIdByUserSlotId = userSlotRepository.findUserIdByUserSlotId(slotId);

        if (userIdByUserSlotId.isPresent()) { //date 테이블에 userId 값이 존재하는지 확인
            if (userIdByUserSlotId.get().equals(userId)) { // 요청자와 슬롯의 주인이 동일 인물인지 확인
                log.info("its okay");
                //슬롯 가져오기
                UserSlot userSlot = userSlotRepository.findById(slotId).orElseThrow(
                        () -> new NotFoundDataException("슬롯이 존재하지 않습니다")
                );
                log.info("its okay2");
                return createMySlotResponseForm(userSlot);
            }
            throw new AccessDeniedException("다른 사용자의 슬롯입니다");
        }
        throw new NotFoundUserException("해당 date에 user id가 할당되지 않았습니다");
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
