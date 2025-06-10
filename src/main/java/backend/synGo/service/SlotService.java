package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import backend.synGo.exception.*;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.form.responseForm.SlotResponseForm;
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

import static backend.synGo.domain.slot.UserSlot.createUserSlot;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlotService {

    private final UserRepository userRepository;
    private final UserSlotRepository userSlotRepository;
    private final DateRepository dateRepository;

    /**
     * 개인 슬롯을 생성하는 서비스
     * @param slotForm
     */
    @Transactional
    public Long createMySlot(SlotForm slotForm, Long userId) {
        if (validDateTime(slotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (slotForm.getEndDate() != null && slotForm.getStartDate().isEqual(slotForm.getEndDate()))
            slotForm.setEndDate(null);

        LocalDate startDate = slotForm.getStartDate().toLocalDate();
        Date date = dateRepository.findDateAndUserSlotByStartDateAndUserId(startDate, userId)   //fetch join
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundUserException("유저 정보 없음"));
                    return new Date(user, startDate);
                });
        //userSlot 생성
        UserSlot userSlot = createUserSlot(
                slotForm.getStatus(),
                slotForm.getTitle(),
                slotForm.getContent(),
                slotForm.getStartDate(),
                slotForm.getEndDate(),
                slotForm.getPlace(),
                slotForm.getImportance(),
                date);
        //date의 SlotCount +1
        date.addSlotCount();
        // cascade로 전부 저장 전파
        userSlotRepository.save(userSlot);
        return userSlot.getId();
    }

    /**
     * 개인 슬롯을 검색하는 서비스
     * @param slotId
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public SlotResponseForm findMySlot(Long slotId, Long userId) {
        //fetch join으로 date, userslot 조회
        Optional<UserSlot> userIdByUserSlotId = userSlotRepository.findDateAndUserSlotByUserSlotId(slotId);

        if (userIdByUserSlotId.isPresent()) { //date 테이블에 userId 값이 존재하는지 확인
            if (userIdByUserSlotId.get().getDate().getUser().getId().equals(userId)) { // 요청자와 슬롯의 주인이 동일 인물인지 확인
                return createMySlotResponseForm(userIdByUserSlotId.get());
            }
            throw new AccessDeniedException("다른 사용자의 슬롯입니다");
        }
        throw new NotFoundDataException("슬롯이 존재하지 않습니다");
    }

    private static SlotResponseForm createMySlotResponseForm(UserSlot userSlot) {
        return SlotResponseForm.builder()
                .slotId(userSlot.getId())
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

    public static boolean validDateTime(SlotForm slotForm) {
        return slotForm.getEndDate() != null && (
                slotForm.getStartDate().isAfter(slotForm.getEndDate()) ||
                slotForm.getStartDate().isBefore(LocalDateTime.now()) ||
                slotForm.getEndDate().isBefore(LocalDateTime.now())
        );
    }
}
