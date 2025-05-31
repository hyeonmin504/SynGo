package backend.synGo.controller.date;

import backend.synGo.service.DateService;
import lombok.RequiredArgsConstructor;;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class DateController {

    private DateService dateService;
}
