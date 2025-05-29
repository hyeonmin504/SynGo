package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.MySlotForm;
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
public class GenerateSlotService {

    private final UserSlotRepository userSlotRepository;
    private final DateRepository dateRepository;
    private final UserService userService;
    private final DateService dateService;


    /**
     * 개인 슬롯을 생성하는 서비스
     * @param mySlotForm
     */
    @Transactional
    public void generateMySlot(MySlotForm mySlotForm) {
        if (validDateTime(mySlotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (mySlotForm.getEndDate() != null && mySlotForm.getStartDate().isEqual(mySlotForm.getEndDate()))
            mySlotForm.setEndDate(null);

        //유저 정보를 받아옵니다
        User user = userService.findUserById(Long.parseLong(mySlotForm.getUserId()));

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
    }

    public static boolean validDateTime(MySlotForm mySlotForm) {
        return mySlotForm.getEndDate() != null && (
                mySlotForm.getStartDate().isAfter(mySlotForm.getEndDate()) ||
                mySlotForm.getStartDate().isBefore(LocalDateTime.now()) ||
                mySlotForm.getEndDate().isBefore(LocalDateTime.now())
        );
    }
}
