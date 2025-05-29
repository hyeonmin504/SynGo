package backend.synGo.controller;

import backend.synGo.domain.slot.Status;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.MySlotForm;
import backend.synGo.repository.UserSlotRepository;
import backend.synGo.service.GenerateSlotService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/my/scheduler")
@RequiredArgsConstructor
public class SlotController {

    private final UserSlotRepository userSlotRepository;
    private final GenerateSlotService generateSlotService ;

//    @PostMapping("{date}")
//    public ResponseEntity<ResponseForm<?>> generateDateContent(@PathVariable LocalDateTime dateTime, @RequestBody )

    @PostMapping("/slot")
    public ResponseEntity<ResponseForm<?>> generateMySlot(@Validated @RequestBody MySlotForm mySlotForm) {
        try {
            generateSlotService.generateMySlot(mySlotForm);
            return ResponseEntity.ok().body(ResponseForm.success(null,"슬롯 생성 성공"));
        } catch (NotValidException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.success(null,e.getMessage()));
        } catch (NotFoundUserException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.success(null,e.getMessage()));
        }
    }
}
