package fr.lostaria.wakeapi.ws;

import fr.lostaria.wakeapi.core.InstanceStatus;
import fr.lostaria.wakeapi.core.exception.OvhApiException;
import fr.lostaria.wakeapi.services.OvhApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceStatusBroadcaster {

    private final OvhApiService ovhApiService;
    private final SimpMessagingTemplate messagingTemplate;
    private InstanceStatus lastStatus = null;

    public InstanceStatusBroadcaster(OvhApiService ovhApiService, SimpMessagingTemplate messagingTemplate) {
        this.ovhApiService = ovhApiService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void broadcastInstanceStatus() {
        try {
            InstanceStatus currentStatus = ovhApiService.getInstanceStatus();
            if (lastStatus == null || !lastStatus.equals(currentStatus)) {
                log.info("Broadcasting instance status change: {} -> {}", lastStatus, currentStatus);
                messagingTemplate.convertAndSend("/topic/instance/status", currentStatus.toString());
                lastStatus = currentStatus;
            }
        } catch (OvhApiException e) {
            log.error("Failed to fetch instance status for WebSocket broadcast", e);
        }
    }
}
