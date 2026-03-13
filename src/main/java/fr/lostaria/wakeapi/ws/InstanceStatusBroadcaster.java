package fr.lostaria.wakeapi.ws;

import fr.lostaria.wakeapi.core.InstanceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceStatusBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public InstanceStatusBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(InstanceStatus status) {
        log.info("Broadcasting instance status: {}", status);
        messagingTemplate.convertAndSend("/topic/instance/status", status.toString());
    }
}
