package backend.synGo.service;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.group.Group;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static backend.synGo.service.SlotService.validDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupSlotService {
    private final UserGroupRepository userGroupRepository;
    private final DateService dateService;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final GroupSlotRepository groupSlotRepository;

    @Transactional
    public Long createGroupSlot(Long groupId, SlotForm slotForm, Long userId) {
        checkUserGroupRole(groupId, userId);

        if (validDateTime(slotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (slotForm.getEndDate() != null && slotForm.getStartDate().isEqual(slotForm.getEndDate()))
            slotForm.setEndDate(null);

        LocalDate startDate = slotForm.getStartDate().toLocalDate();
        log.info("startDate={}", startDate);
        Date date = dateRepository.findDateAndGroupSlotByStartDateAndUserId(startDate, groupId)   //fetch join
                .orElseGet(() -> {
                    Group group = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundUserException("그룹 정보 없음"));
                    return new Date(group, startDate);
                });
        log.info("date={}", date);
        log.info("slotForm: {}", slotForm);
        //groupSlot 생성
        GroupSlot groupSlot = GroupSlot.createGroupSlot(
                slotForm.getStatus(),
                slotForm.getTitle(),
                slotForm.getContent(),
                slotForm.getStartDate(),
                slotForm.getEndDate(),
                slotForm.getPlace(),
                slotForm.getImportance(),
                date);
        //date의 SlotCount, summary를 업데이트
        dateService.updateGroupDateInfo(date, groupSlot);
        groupSlotRepository.save(groupSlot);
        return groupSlot.getId();
    }

    private void checkUserGroupRole(Long groupId, Long userId) {
        UserGroup userGroup = userGroupRepository.findByGroupIdAndUserId(userId, groupId).orElseThrow(
                () -> new AccessDeniedException("그룹원 외 접근 불가")
        );
        if (userGroup.getRole() != Role.LEADER && userGroup.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("권한 부족");
        }
    }
}
