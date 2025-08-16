package fr.lostaria.wakeapi.controllers;

import fr.lostaria.wakeapi.payload.APIResponse;
import fr.lostaria.wakeapi.services.MinecraftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/minecraft")
public class MinecraftController {

    private MinecraftService minecraftService;

    public MinecraftController(MinecraftService minecraftService) {
        this.minecraftService = minecraftService;
    }

    @GetMapping("/status")
    public ResponseEntity status() {
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "SERVER_ONLINE", "" + minecraftService.isServerOnline()));
    }

    @GetMapping("/players")
    public ResponseEntity players() {
        if(!minecraftService.isServerOnline()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new APIResponse(false, "SERVER_OFFLINE", "Server is offline"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "ONLINE_PLAYERS", "" + minecraftService.getOnlinePlayersCount()));
    }

}
