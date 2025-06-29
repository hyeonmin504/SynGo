package backend.synGo.service;

import backend.synGo.form.requestForm.SlotForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SlotServiceTest {

    private SlotForm makeForm(LocalDateTime start, LocalDateTime end) {
        SlotForm form = new SlotForm();
        form.setStartDate(start);
        form.setEndDate(end);
        return form;
    }

    @Test
    @DisplayName("시작 시간이 끝 시간보다 늦은 경우 - 예외 발생")
    void testInvalidDate_startAfterEnd() {
        LocalDateTime now = LocalDateTime.now();
        SlotForm form = makeForm(now.plusDays(1), now);

        assertTrue(SlotService.validDateTime(form.getStartDate(), form.getEndDate()));
    }

    @Test
    @DisplayName("시작 시간이 현재보다 과거인 경우 - 예외 발생")
    void testInvalidDate_startInPast() {
        LocalDateTime now = LocalDateTime.now();
        SlotForm form = makeForm(now.minusDays(1), now.plusDays(1));

        assertTrue(SlotService.validDateTime(form.getStartDate(), form.getEndDate()));
    }

    @Test
    @DisplayName("끝 시간이 현재보다 과거인 경우 - 예외 발생")
    void testInvalidDate_endInPast() {
        LocalDateTime now = LocalDateTime.now();
        SlotForm form = makeForm(now.plusHours(1), now.minusDays(1));

        assertTrue(SlotService.validDateTime(form.getStartDate(), form.getEndDate()));
    }

    @Test
    @DisplayName("정상적인 날짜 입력 - 예외 없음")
    void testValidDate() {
        LocalDateTime now = LocalDateTime.now();
        SlotForm form = makeForm(now.plusHours(1), now.plusHours(2));

        assertFalse(SlotService.validDateTime(form.getStartDate(), form.getEndDate()));
    }

    @Test
    @DisplayName("끝 시간이 null이고 시작 시간이 미래인 경우 - 예외 없음")
    void testValidDate_endDateNull() {
        LocalDateTime now = LocalDateTime.now();
        SlotForm form = makeForm(now.plusDays(1), null);

        assertFalse(SlotService.validDateTime(form.getStartDate(), form.getEndDate()));
    }

}