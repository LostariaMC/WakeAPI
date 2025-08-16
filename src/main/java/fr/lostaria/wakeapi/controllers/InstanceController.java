package fr.lostaria.wakeapi.controllers;

import fr.lostaria.wakeapi.core.InstanceStatus;
import fr.lostaria.wakeapi.core.exception.OvhApiException;
import fr.lostaria.wakeapi.payload.APIResponse;
import fr.lostaria.wakeapi.services.InstanceWatchService;
import fr.lostaria.wakeapi.services.OvhApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/instance")
public class InstanceController {

    private OvhApiService ovhApiService;
    private final InstanceWatchService watchService;

    public InstanceController(OvhApiService ovhApiService, InstanceWatchService watchService) {
        this.ovhApiService = ovhApiService;
        this.watchService = watchService;
    }

    @PostMapping("/start")
    public ResponseEntity start() throws OvhApiException {
        InstanceStatus status = ovhApiService.getInstanceStatus();
        if(status.isStarting() || status.isStopping()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new APIResponse(false, "INSTANCE_ALREADY_STARTING_OR_STOPPING", "L'instance est déjà en cours de démarrage ou d'arrêt"));
        }
        if(status.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new APIResponse(false, "INSTANCE_ALREADY_ACTIVE", "L'instance est déjà démarrée"));
        }
        ovhApiService.unshelveInstance();
        watchService.startWatchAfterOneHour();
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "INSTANCE_STARTING", "Instance en cours de démarrage"));
    }

    @PostMapping("/stop")
    public ResponseEntity stop() throws OvhApiException, IOException {
        InstanceStatus status = ovhApiService.getInstanceStatus();
        if(status.isStarting() || status.isStopping()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new APIResponse(false, "INSTANCE_ALREADY_STARTING_OR_STOPPING", "L'instance est déjà en cours de démarrage ou d'arrêt"));
        }
        if(!status.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new APIResponse(false, "INSTANCE_NOT_ACTIVE", "L'instance est déjà éteinte"));
        }
        ovhApiService.shelveInstance();
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "INSTANCE_STOPPING", "Instance en cours d'arrêt"));
    }

    @GetMapping("/status")
    public ResponseEntity status() throws OvhApiException {
        InstanceStatus status = ovhApiService.getInstanceStatus();
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "INSTANCE_STATUS", status.toString()));
    }

}
