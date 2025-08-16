package fr.lostaria.wakeapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WakeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WakeApiApplication.class, args);
	}

}
