package backend.synGo.syncalender.service;

import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.auth.service.GoogleLinkService;
import backend.synGo.domain.user.Provider;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.repository.UserOAuthConnectionRepository;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static backend.synGo.controller.my.MySlotController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncCalenderService {

    private final UserOAuthConnectionRepository userOAuthConnectionRepository;
    private final GoogleLinkService googleLinkService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Transactional
    public void syncToGoogleCalender(UserSlotResponseForm mySlot, Long userId) {
        try {
            // 1. Google Credential 얻기
            Credential credential = getGoogleCredential(userId);

            // 2. Calendar Service 생성
            Calendar calendarService = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("syngo") // 앱 이름으로 변경
                    .build();

            // 3. 슬롯을 Google Event로 변환
            Event event = convertSlotToEvent(mySlot);

            // 4. Google Calendar에 이벤트 추가
            Event createdEvent = calendarService.events()
                    .insert("primary", event) // "primary"는 기본 캘린더
                    .execute();

            log.info("Google Calendar 이벤트 생성 완료. Event ID: {}", createdEvent.getId());

        } catch (Exception e) {
            log.error("Google Calendar 동기화 실패", e);
            throw new RuntimeException("캘린더 동기화에 실패했습니다: " + e.getMessage());
        }
    }

    private Event convertSlotToEvent(UserSlotResponseForm slot) {
        Event event = new Event();

        // 제목 설정
        event.setSummary(slot.getTitle());

        // 내용 설정
        event.setDescription(slot.getContent());

        // 장소 설정
        if (slot.getPlace() != null) {
            event.setLocation(slot.getPlace());
        }

        // 시작 시간 설정
        EventDateTime startDateTime = new EventDateTime()
                .setDateTime(convertToGoogleDateTime(slot.getStartDate()))
                .setTimeZone("Asia/Seoul");
        event.setStart(startDateTime);

        // 종료 시간 설정
        EventDateTime endDateTime = new EventDateTime()
                .setDateTime(convertToGoogleDateTime(slot.getEndDate()))
                .setTimeZone("Asia/Seoul");
        event.setEnd(endDateTime);

        return event;
    }

    private com.google.api.client.util.DateTime convertToGoogleDateTime(LocalDateTime localDateTime) {
        return new com.google.api.client.util.DateTime(
                Date.from(localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant())
        );
    }

    /**
     * 사용자 ID로 Google Credential 객체 생성
     */
    public Credential getGoogleCredential(Long userId) {
        UserOAuthConnection oAuthConnection = userOAuthConnectionRepository
                .findByUserIdAndProvider(userId, Provider.GOOGLE)
                .orElseThrow(() -> new NotFoundUserException("Google OAuth 연결 정보를 찾을 수 없습니다."));

        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(new NetHttpTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .setClientAuthentication(new ClientParametersAuthentication(
                        googleClientId,
                        googleClientSecret
                ))
                .build()
                .setAccessToken(oAuthConnection.getAccessToken())
                .setRefreshToken(oAuthConnection.getRefreshToken())
                .setExpirationTimeMilliseconds(
                        oAuthConnection.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                );

    }
}
