package fr.lostaria.wakeapi.controllers;

import fr.lostaria.wakeapi.payload.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/ping")
    public ResponseEntity ping() {
        return ResponseEntity.status(HttpStatus.OK).body(new APIResponse(true, "PONG", "Pong"));
    }

}
