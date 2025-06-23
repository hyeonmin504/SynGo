package backend.synGo.webSocket.controller;

import backend.synGo.webSocket.messagingStompWebocket.Greeting;
import backend.synGo.webSocket.messagingStompWebocket.HelloMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class GroupSynGoController {
     @MessageMapping("/hello")
     @SendTo("/topic/greetings")
     public Greeting handleGroupMessage(HelloMessage message) throws InterruptedException {
         Thread.sleep(1000);
         return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
     }
}
