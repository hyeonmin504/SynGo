package backend.synGo.syncalender.service;

import backend.synGo.controller.my.MySlotController;
import backend.synGo.repository.UserOAuthConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static backend.synGo.controller.my.MySlotController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncCalenderService {

    private final UserOAuthConnectionRepository userOAuthConnectionRepository;

    @Transactional
    public void syncToGoogleCalender(UserSlotResponseForm mySlot) {
        //슬롯 -> 구글 캘린더 폼 컨버터

        //accessToken 확인 및 발급

        //google calender api 요청

        //응답 후 처리
    }
}
